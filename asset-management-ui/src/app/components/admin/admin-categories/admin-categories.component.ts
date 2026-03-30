import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AssetService } from '../../../services/asset.service';
import { CategoryResponse, CategoryRequest, AttributeRequest, AttributeDataType } from '../../../models/asset.model';

interface AttributeFormRow {
  name: string;
  dataType: AttributeDataType;
}

@Component({
  selector: 'app-admin-categories',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-categories.component.html',
  styleUrl: './admin-categories.component.css'
})
export class AdminCategoriesComponent implements OnInit {
  categories = signal<CategoryResponse[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  selectedCategory = signal<CategoryResponse | null>(null);
  detailLoading = signal(false);

  showModal = signal(false);
  saving = signal(false);
  modalError = signal<string | null>(null);

  deletingCategory = signal<{ catId: number; attrId: number; attrName: string } | null>(null);

  formName = '';
  formDescription = '';
  formParentId: number | '' = '';
  formAttributes: AttributeFormRow[] = [{ name: '', dataType: 'STRING' }];

  readonly dataTypes: AttributeDataType[] = ['STRING', 'NUMBER', 'DATE', 'BOOLEAN'];

  constructor(private assetService: AssetService) {}

  ngOnInit(): void { this.loadCategories(); }

  loadCategories(): void {
    this.loading.set(true);
    this.assetService.getAllCategories().subscribe({
      next: cats => { this.categories.set(cats); this.loading.set(false); },
      error: () => { this.error.set('Failed to load categories.'); this.loading.set(false); }
    });
  }


  openDetail(cat: CategoryResponse): void {
    this.selectedCategory.set(cat);
    this.detailLoading.set(true);
    this.assetService.getCategoryById(cat.id).subscribe({
      next: full => { this.selectedCategory.set(full); this.detailLoading.set(false); },
      error: () => { this.detailLoading.set(false); }
    });
  }

  closeDetail(): void { this.selectedCategory.set(null); }

  parentName(parentId: number | null): string {
    if (!parentId) return '';
    return this.categories().find(c => c.id === parentId)?.name ?? String(parentId);
  }


  openCreate(): void {
    this.formName = '';
    this.formDescription = '';
    this.formParentId = '';
    this.formAttributes = [{ name: '', dataType: 'STRING' }];
    this.modalError.set(null);
    this.showModal.set(true);
  }

  closeModal(): void { this.showModal.set(false); }

  addAttributeRow(): void { this.formAttributes.push({ name: '', dataType: 'STRING' }); }
  removeAttributeRow(i: number): void { this.formAttributes.splice(i, 1); }

  save(): void {
    if (!this.formName.trim() || !this.formDescription.trim()) {
      this.modalError.set('Name and description are required.'); return;
    }
    const attrs: AttributeRequest[] = this.formAttributes
      .filter(a => a.name.trim())
      .map(a => ({ name: a.name.trim(), dataType: a.dataType }));

    const req: CategoryRequest = {
      name: this.formName.trim(),
      description: this.formDescription.trim(),
      parentCategoryId: this.formParentId !== '' ? Number(this.formParentId) : null,
      attributeRequestList: attrs
    };

    this.saving.set(true);
    this.modalError.set(null);
    this.assetService.createCategory(req).subscribe({
      next: () => { this.saving.set(false); this.closeModal(); this.loadCategories(); },
      error: err => { this.saving.set(false); this.modalError.set(err?.error?.message ?? 'Failed to create category.'); }
    });
  }


  confirmDeleteAttr(catId: number, attrId: number, attrName: string): void {
    this.deletingCategory.set({ catId, attrId, attrName });
  }
  cancelDeleteAttr(): void { this.deletingCategory.set(null); }

  deleteAttribute(): void {
    const d = this.deletingCategory();
    if (!d) return;
    this.assetService.deleteCategoryAttribute(d.catId, d.attrId).subscribe({
      next: () => {
        this.deletingCategory.set(null);
        this.loadCategories();
        const sel = this.selectedCategory();
        if (sel && sel.id === d.catId) this.openDetail(sel);
      },
      error: () => { this.deletingCategory.set(null); this.error.set('Failed to remove attribute.'); }
    });
  }
}
