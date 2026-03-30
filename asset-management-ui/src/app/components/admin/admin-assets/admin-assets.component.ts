import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';
import { AssetService } from '../../../services/asset.service';
import {
  AssetResponse, CategoryResponse, AssetStatus, AttributeDataType,
  AssetRequest, AssetAttributeValueRequest, AssetFilterRequest,
  AssetAttributeValueResponse, AttributeResponse
} from '../../../models/asset.model';

interface AttributeFormRow {
  attributeId: number;
  name: string;
  dataType: AttributeDataType;
  valueString: string;
  valueNumber: string;
  valueDate: string;
  valueBool: boolean;
}

interface AttributeFilterRow {
  attribute: AttributeResponse;
  valueString: string;
  valueNumber: string;
  valueDate: string;
  valueBool: boolean;
}

@Component({
  selector: 'app-admin-assets',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin-assets.component.html',
  styleUrl: './admin-assets.component.css'
})
export class AdminAssetsComponent implements OnInit {
  readonly maxImages = 10;

  assets = signal<AssetResponse[]>([]);
  categories = signal<CategoryResponse[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  pageSize = 10;

  showModal = signal(false);
  editingAsset = signal<AssetResponse | null>(null);
  saving = signal(false);
  modalError = signal<string | null>(null);
  imageLimitError = signal<string | null>(null);
  selectedFiles: File[] = [];

  deletingId = signal<number | null>(null);

  formAssetCode = '';
  formName = '';
  formDescription = '';
  formStatus: AssetStatus | '' = 'ACTIVE';
  formCategoryId: number | '' = '';
  formAttributeRows: AttributeFormRow[] = [];
  attrLoading = signal(false);

  readonly statuses: AssetStatus[] = ['ACTIVE', 'IN_USE', 'DAMAGED', 'RETIRED'];

  get formParentCategories(): CategoryResponse[] {
    const parentIds = new Set(this.categories().filter(c => c.parentCategoryId).map(c => c.parentCategoryId!));
    return this.categories().filter(c => !c.parentCategoryId && parentIds.has(c.id));
  }
  get formStandaloneCategories(): CategoryResponse[] {
    const parentIds = new Set(this.categories().filter(c => c.parentCategoryId).map(c => c.parentCategoryId!));
    return this.categories().filter(c => !c.parentCategoryId && !parentIds.has(c.id));
  }
  formSubCategoriesOf(parentId: number): CategoryResponse[] {
    return this.categories().filter(c => c.parentCategoryId === parentId);
  }

  searchQuery = '';
  private searchSubject = new Subject<string>();
  private mode: 'all' | 'search' | 'filter' = 'all';
  private activeFilter: AssetFilterRequest = {};

  showFilters = signal(false);
  filterName = '';
  filterAssetCode = '';
  filterStatus: AssetStatus | '' = '';
  filterCategoryId: number | '' = '';
  attributeFilterRows: AttributeFilterRow[] = [];

  constructor(private assetService: AssetService, private router: Router) {}

  ngOnInit(): void {
    this.loadAssets();
    this.assetService.getAllCategories().subscribe({ next: cats => this.categories.set(cats) });

    this.searchSubject.pipe(debounceTime(400), distinctUntilChanged()).subscribe(q => {
      if (q.trim().length === 0) { this.mode = 'all'; this.loadAssets(0); }
      else { this.mode = 'search'; this.doSearch(q, 0); }
    });
  }

  onSearchInput(): void { this.searchSubject.next(this.searchQuery); }

  clearSearch(): void { this.searchQuery = ''; this.mode = 'all'; this.loadAssets(0); }


  onFilterCategoryChange(): void {
    if (this.filterCategoryId === '') { this.attributeFilterRows = []; return; }
    this.assetService.getCategoryById(+this.filterCategoryId).subscribe({
      next: cat => {
        this.attributeFilterRows = (cat.attributes ?? []).map(attr => ({
          attribute: attr, valueString: '', valueNumber: '', valueDate: '', valueBool: false
        }));
      },
      error: () => { this.attributeFilterRows = []; }
    });
  }

  get filterParentCategories(): CategoryResponse[] {
    const parentIds = new Set(this.categories().filter(c => c.parentCategoryId).map(c => c.parentCategoryId!));
    return this.categories().filter(c => !c.parentCategoryId && parentIds.has(c.id));
  }
  get filterStandaloneCategories(): CategoryResponse[] {
    const parentIds = new Set(this.categories().filter(c => c.parentCategoryId).map(c => c.parentCategoryId!));
    return this.categories().filter(c => !c.parentCategoryId && !parentIds.has(c.id));
  }
  filterSubCategoriesOf(parentId: number): CategoryResponse[] {
    return this.categories().filter(c => c.parentCategoryId === parentId);
  }

  applyFilters(): void {
    const assetAttributes: AssetAttributeValueResponse[] = this.attributeFilterRows
      .filter(row => {
        switch (row.attribute.dataType) {
          case 'STRING': return row.valueString.trim() !== '';
          case 'NUMBER': return row.valueNumber.trim() !== '';
          case 'DATE':   return row.valueDate.trim() !== '';
          case 'BOOLEAN': return true;
          default: return false;
        }
      })
      .map(row => ({
        attribute: row.attribute,
        valueString: row.attribute.dataType === 'STRING' ? row.valueString || null : null,
        valueNumber: row.attribute.dataType === 'NUMBER' ? (row.valueNumber !== '' ? +row.valueNumber : null) : null,
        valueDate:   row.attribute.dataType === 'DATE'   ? row.valueDate || null : null,
        valueBool:   row.attribute.dataType === 'BOOLEAN' ? row.valueBool : null
      }));

    const selectedCat = this.filterCategoryId !== ''
      ? this.categories().find(c => c.id === +this.filterCategoryId) : null;

    const f: AssetFilterRequest = {
      name: this.filterName || null,
      assetCode: this.filterAssetCode || null,
      categoryName: selectedCat?.name || null,
      assetStatus: this.filterStatus || null,
      assetAttributes: assetAttributes.length > 0 ? assetAttributes : null
    };

    const isEmpty = !f.name && !f.assetCode && !f.categoryName && !f.assetStatus && !f.assetAttributes;
    if (isEmpty) { this.mode = 'all'; this.loadAssets(0); }
    else { this.mode = 'filter'; this.activeFilter = f; this.doFilter(f, 0); }
  }

  clearFilters(): void {
    this.filterName = '';
    this.filterAssetCode = '';
    this.filterStatus = '';
    this.filterCategoryId = '';
    this.attributeFilterRows = [];
    this.activeFilter = {};
    this.mode = 'all';
    this.loadAssets(0);
  }

  get hasActiveFilters(): boolean {
    return !!(this.filterName || this.filterAssetCode || this.filterStatus || this.filterCategoryId !== '');
  }


  loadAssets(page = 0): void {
    this.loading.set(true);
    this.error.set(null);
    this.assetService.getAll(page, this.pageSize).subscribe({
      next: res => this.handlePagedResponse(res),
      error: () => { this.error.set('Failed to load assets.'); this.loading.set(false); }
    });
  }

  private doSearch(query: string, page: number): void {
    this.loading.set(true); this.error.set(null);
    this.assetService.search(query, page, this.pageSize).subscribe({
      next: res => this.handlePagedResponse(res),
      error: () => { this.error.set('Failed to search assets.'); this.loading.set(false); }
    });
  }

  private doFilter(f: AssetFilterRequest, page: number): void {
    this.loading.set(true); this.error.set(null);
    this.assetService.filter(f, page, this.pageSize).subscribe({
      next: res => this.handlePagedResponse(res),
      error: () => { this.error.set('Failed to filter assets.'); this.loading.set(false); }
    });
  }

  private handlePagedResponse(res: any): void {
    const embedded = res._embedded;
    const list = embedded ? (Object.values(embedded)[0] as AssetResponse[]) ?? [] : [];
    this.assets.set(list);
    this.currentPage.set(res.page.number);
    this.totalPages.set(res.page.totalPages);
    this.totalElements.set(res.page.totalElements);
    this.loading.set(false);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    if (this.mode === 'search') this.doSearch(this.searchQuery, page);
    else if (this.mode === 'filter') this.doFilter(this.activeFilter, page);
    else this.loadAssets(page);
  }


  viewDetails(id: number): void {
    this.router.navigate(['/admin/assets', id]);
  }

  openCreate(): void {
    this.editingAsset.set(null);
    this.resetForm();
    this.showModal.set(true);
  }

  openEdit(asset: AssetResponse): void {
    this.editingAsset.set(asset);
    this.formAssetCode = asset.assetCode;
    this.formName = asset.name;
    this.formDescription = asset.description;
    this.formStatus = asset.assetStatus;
    this.formCategoryId = asset.category.id;
    this.formAttributeRows = [];
    this.showModal.set(true);
    this.attrLoading.set(true);
    this.assetService.getById(asset.id).subscribe({
      next: full => {
        this.editingAsset.set(full);
        this.buildAttributeRows(full.category.id, full.attributeValues ?? []);
      },
      error: () => {
        this.buildAttributeRows(asset.category.id, asset.attributeValues ?? []);
      }
    });
  }

  closeModal(): void {
    this.showModal.set(false);
    this.modalError.set(null);
    this.imageLimitError.set(null);
    this.selectedFiles = [];
  }

  resetForm(): void {
    this.formAssetCode = ''; this.formName = ''; this.formDescription = '';
    this.formStatus = 'ACTIVE'; this.formCategoryId = ''; this.formAttributeRows = []; this.selectedFiles = [];
    this.imageLimitError.set(null);
  }

  get isImageLimitReached(): boolean {
    return this.selectedFiles.length >= this.maxImages;
  }

  get nonBoolAttributeRows(): AttributeFormRow[] {
    return this.formAttributeRows.filter(r => r.dataType !== 'BOOLEAN');
  }
  get boolAttributeRows(): AttributeFormRow[] {
    return this.formAttributeRows.filter(r => r.dataType === 'BOOLEAN');
  }

  onCategoryChange(): void {
    if (this.formCategoryId === '') { this.formAttributeRows = []; return; }
    this.attrLoading.set(true);
    this.assetService.getCategoryById(Number(this.formCategoryId)).subscribe({
      next: cat => {
        this.formAttributeRows = (cat.attributes ?? []).map(attr => ({
          attributeId: attr.id, name: attr.name, dataType: attr.dataType as AttributeDataType,
          valueString: '', valueNumber: '', valueDate: '', valueBool: false
        }));
        this.attrLoading.set(false);
      },
      error: () => { this.formAttributeRows = []; this.attrLoading.set(false); }
    });
  }

  private buildAttributeRows(categoryId: number, existingValues: AssetAttributeValueResponse[] = []): void {
    this.attrLoading.set(true);
    this.assetService.getCategoryById(categoryId).subscribe({
      next: cat => {
        this.formAttributeRows = (cat.attributes ?? []).map(attr => {
          const existing = existingValues.find(av => av.attribute.id === attr.id);
          return {
            attributeId: attr.id,
            name: attr.name,
            dataType: attr.dataType as AttributeDataType,
            valueString: existing?.valueString ?? '',
            valueNumber: existing?.valueNumber !== null && existing?.valueNumber !== undefined ? String(existing.valueNumber) : '',
            valueDate:   existing?.valueDate ?? '',
            valueBool:   existing?.valueBool ?? false
          };
        });
        this.attrLoading.set(false);
      },
      error: () => { this.formAttributeRows = []; this.attrLoading.set(false); }
    });
  }

  onFilesChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const incoming = input.files ? Array.from(input.files) : [];
    if (incoming.length === 0) return;

    const existingKeys = new Set(this.selectedFiles.map(f => this.fileKey(f)));
    const uniqueIncoming: File[] = [];
    for (const file of incoming) {
      const key = this.fileKey(file);
      if (!existingKeys.has(key)) {
        uniqueIncoming.push(file);
        existingKeys.add(key);
      }
    }

    const slots = Math.max(0, this.maxImages - this.selectedFiles.length);
    if (slots === 0) {
      this.imageLimitError.set(`Maximum ${this.maxImages} images allowed.`);
      input.value = '';
      return;
    }

    this.selectedFiles.push(...uniqueIncoming.slice(0, slots));
    if (uniqueIncoming.length > slots) {
      this.imageLimitError.set(`Maximum ${this.maxImages} images allowed. Extra files were ignored.`);
    } else {
      this.imageLimitError.set(null);
    }

    // Allow selecting the same file again in the next picker interaction.
    input.value = '';
  }

  removeSelectedFile(index: number): void {
    if (index < 0 || index >= this.selectedFiles.length) return;
    this.selectedFiles.splice(index, 1);
    if (!this.isImageLimitReached) this.imageLimitError.set(null);
  }

  clearSelectedFiles(): void {
    this.selectedFiles = [];
    this.imageLimitError.set(null);
  }

  private fileKey(file: File): string {
    return `${file.name}::${file.size}::${file.lastModified}`;
  }

  save(): void {
    if (!this.formAssetCode.trim() || !this.formName.trim() || !this.formDescription.trim() || this.formCategoryId === '') {
      this.modalError.set('Please fill in all required fields.'); return;
    }
    if (!this.editingAsset() && this.selectedFiles.length > this.maxImages) {
      this.modalError.set(`You can upload a maximum of ${this.maxImages} images.`);
      return;
    }
    this.saving.set(true); this.modalError.set(null);

    const attrValues: AssetAttributeValueRequest[] = this.formAttributeRows.map(row => {
      const val: AssetAttributeValueRequest = { attributeId: row.attributeId };
      if (row.dataType === 'STRING')  val.valueString = row.valueString || null;
      if (row.dataType === 'NUMBER')  val.valueNumber = row.valueNumber ? Number(row.valueNumber) : null;
      if (row.dataType === 'DATE')    val.valueData   = row.valueDate || null;
      if (row.dataType === 'BOOLEAN') val.valueBool   = row.valueBool;
      return val;
    });

    const req: AssetRequest = {
      assetCode: this.formAssetCode.trim(), name: this.formName.trim(),
      description: this.formDescription.trim(), assetStatus: this.formStatus || null,
      categoryId: Number(this.formCategoryId),
      assetAttributeValue: attrValues.length > 0 ? attrValues : null
    };

    const editing = this.editingAsset();
    if (editing) {
      this.assetService.updateAsset(editing.id, req).subscribe({
        next: () => { this.saving.set(false); this.closeModal(); this.goToPage(this.currentPage()); },
        error: err => { this.saving.set(false); this.modalError.set(err?.error?.message ?? 'Failed to update asset.'); }
      });
    } else {
      this.assetService.createAsset(req, this.selectedFiles).subscribe({
        next: () => { this.saving.set(false); this.closeModal(); this.loadAssets(0); },
        error: err => { this.saving.set(false); this.modalError.set(err?.error?.message ?? 'Failed to create asset.'); }
      });
    }
  }


  confirmDelete(id: number): void { this.deletingId.set(id); }
  cancelDelete(): void            { this.deletingId.set(null); }

  deleteAsset(): void {
    const id = this.deletingId();
    if (!id) return;
    this.assetService.deleteAsset(id).subscribe({
      next: () => { this.deletingId.set(null); this.goToPage(this.currentPage()); },
      error: () => { this.deletingId.set(null); this.error.set('Failed to delete asset.'); }
    });
  }


  get visiblePages(): (number | '...')[] {
    const total = this.totalPages();
    const cur   = this.currentPage();
    if (total <= 7) return Array.from({ length: total }, (_, i) => i);
    const pages: (number | '...')[] = [0];
    if (cur > 2)         pages.push('...');
    for (let i = Math.max(1, cur - 1); i <= Math.min(total - 2, cur + 1); i++) pages.push(i);
    if (cur < total - 3) pages.push('...');
    pages.push(total - 1);
    return pages;
  }

  statusLabel(s: AssetStatus): string {
    return { ACTIVE: 'Active', IN_USE: 'In Use', DAMAGED: 'Damaged', RETIRED: 'Retired' }[s] ?? s;
  }
  statusClass(s: AssetStatus): string {
    return { ACTIVE: 'st-active', IN_USE: 'st-inuse', DAMAGED: 'st-damaged', RETIRED: 'st-retired' }[s] ?? '';
  }
}
