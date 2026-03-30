import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';
import { AssetService } from '../../services/asset.service';
import {
  AssetResponse, PagedModel, AssetStatus, AssetFilterRequest,
  CategoryResponse, AttributeResponse, AssetAttributeValueResponse
} from '../../models/asset.model';
import { AssetCardComponent } from '../asset-card/asset-card.component';
import { AuthService } from '../../services/auth.service';

export interface AttributeFilterRow {
  attribute: AttributeResponse;
  valueString: string;
  valueNumber: string;
  valueDate: string;
  valueBool: boolean;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AssetCardComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit {
  assets = signal<AssetResponse[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  categories = signal<CategoryResponse[]>([]);

  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  pageSize = 12;

  searchQuery = '';
  private searchSubject = new Subject<string>();

  showFilters = signal(false);
  filterName = '';
  filterAssetCode = '';
  filterStatus: AssetStatus | '' = '';
  filterCategoryId: number | '' = '';
  attributeFilterRows: AttributeFilterRow[] = [];

  readonly statuses: AssetStatus[] = ['ACTIVE', 'IN_USE', 'DAMAGED', 'RETIRED'];

  private mode: 'all' | 'search' | 'filter' = 'all';
  private activeFilter: AssetFilterRequest = {};

  constructor(private assetService: AssetService, public authService: AuthService) {}

  ngOnInit(): void {
    this.loadCategories();
    this.loadAssets();
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(q => {
      if (q.trim().length === 0) {
        this.mode = 'all';
        this.loadAssets(0);
      } else {
        this.mode = 'search';
        this.doSearch(q, 0);
      }
    });
  }

  loadCategories(): void {
    this.assetService.getAllCategories().subscribe({
      next: cats => this.categories.set(cats),
      error: () => {}
    });
  }

  subCategoriesOf(parentId: number): CategoryResponse[] {
    return this.categories().filter(c => c.parentCategoryId === parentId);
  }

  get standaloneCategories(): CategoryResponse[] {
    const parentIds = new Set(
      this.categories().filter(c => c.parentCategoryId).map(c => c.parentCategoryId!)
    );
    return this.categories().filter(c => !c.parentCategoryId && !parentIds.has(c.id));
  }

  get groupedParentCategories(): CategoryResponse[] {
    const parentIds = new Set(
      this.categories().filter(c => c.parentCategoryId).map(c => c.parentCategoryId!)
    );
    return this.categories().filter(c => !c.parentCategoryId && parentIds.has(c.id));
  }

  onSearchInput(): void {
    this.searchSubject.next(this.searchQuery);
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.mode = 'all';
    this.loadAssets(0);
  }

  onCategoryChange(): void {
    if (this.filterCategoryId === '') {
      this.attributeFilterRows = [];
      return;
    }
    this.assetService.getCategoryById(+this.filterCategoryId).subscribe({
      next: cat => {
        this.attributeFilterRows = (cat.attributes ?? []).map(attr => ({
          attribute: attr,
          valueString: '',
          valueNumber: '',
          valueDate: '',
          valueBool: false
        }));
      },
      error: () => { this.attributeFilterRows = []; }
    });
  }

  applyFilters(): void {
    const assetAttributes: AssetAttributeValueResponse[] = this.attributeFilterRows
      .filter(row => {
        switch (row.attribute.dataType) {
          case 'STRING': return row.valueString.trim() !== '';
          case 'NUMBER': return row.valueNumber.trim() !== '';
          case 'DATE': return row.valueDate.trim() !== '';
          case 'BOOLEAN': return true;
          default: return false;
        }
      })
      .map(row => ({
        attribute: row.attribute,
        valueString: row.attribute.dataType === 'STRING' ? row.valueString || null : null,
        valueNumber: row.attribute.dataType === 'NUMBER' ? (row.valueNumber !== '' ? +row.valueNumber : null) : null,
        valueDate: row.attribute.dataType === 'DATE' ? row.valueDate || null : null,
        valueBool: row.attribute.dataType === 'BOOLEAN' ? row.valueBool : null
      }));

    const selectedCat = this.filterCategoryId !== ''
      ? this.categories().find(c => c.id === +this.filterCategoryId)
      : null;

    const f: AssetFilterRequest = {
      name: this.filterName || null,
      assetCode: this.filterAssetCode || null,
      categoryName: selectedCat?.name || null,
      assetStatus: this.filterStatus || null,
      assetAttributes: assetAttributes.length > 0 ? assetAttributes : null
    };

    const isEmpty = !f.name && !f.assetCode && !f.categoryName && !f.assetStatus && !f.assetAttributes;
    if (isEmpty) {
      this.mode = 'all';
      this.loadAssets(0);
    } else {
      this.mode = 'filter';
      this.activeFilter = f;
      this.doFilter(f, 0);
    }
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
      next: data => this.handlePagedResponse(data),
      error: err => this.handleError(err)
    });
  }

  private doSearch(query: string, page: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.assetService.search(query, page, this.pageSize).subscribe({
      next: data => this.handlePagedResponse(data),
      error: err => this.handleError(err)
    });
  }

  private doFilter(f: AssetFilterRequest, page: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.assetService.filter(f, page, this.pageSize).subscribe({
      next: data => this.handlePagedResponse(data),
      error: err => this.handleError(err)
    });
  }

  private handlePagedResponse(data: PagedModel<AssetResponse>): void {
    const embedded = data._embedded;
    const list = embedded ? (Object.values(embedded)[0] as AssetResponse[]) ?? [] : [];
    this.assets.set(list);
    this.currentPage.set(data.page.number);
    this.totalPages.set(data.page.totalPages);
    this.totalElements.set(data.page.totalElements);
    this.loading.set(false);
  }

  private handleError(err: unknown): void {
    console.error(err);
    this.error.set('Failed to load assets. Please make sure the backend is running.');
    this.loading.set(false);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    if (this.mode === 'search') this.doSearch(this.searchQuery, page);
    else if (this.mode === 'filter') this.doFilter(this.activeFilter, page);
    else this.loadAssets(page);
  }

  get visiblePages(): (number | '...')[] {
    const total = this.totalPages();
    const cur = this.currentPage();
    if (total <= 7) return Array.from({ length: total }, (_, i) => i);

    const pages = new Set<number>([0, total - 1]);
    for (let i = Math.max(0, cur - 2); i <= Math.min(total - 1, cur + 2); i++) pages.add(i);

    const sorted = Array.from(pages).sort((a, b) => a - b);
    const result: (number | '...')[] = [];
    for (let i = 0; i < sorted.length; i++) {
      if (i > 0 && sorted[i] - sorted[i - 1] > 1) result.push('...');
      result.push(sorted[i]);
    }
    return result;
  }
}
