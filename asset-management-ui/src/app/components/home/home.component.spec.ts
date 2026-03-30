import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { of, throwError } from 'rxjs';
import { signal } from '@angular/core';
import { HomeComponent } from './home.component';
import { AssetService } from '../../services/asset.service';
import { AuthService } from '../../services/auth.service';
import {
  AssetResponse, CategoryResponse, PagedModel,
} from '../../models/asset.model';

// ── Fixtures ───────────────────────────────────────────────────────────────

const cat1: CategoryResponse = { id: 1, name: 'Electronics', description: '', parentCategoryId: null, attributes: [] };
const cat2: CategoryResponse = { id: 2, name: 'Laptops', description: '', parentCategoryId: 1, attributes: [] };
const cat3: CategoryResponse = { id: 3, name: 'Furniture', description: '', parentCategoryId: null, attributes: [] };

const mockAsset: AssetResponse = {
  id: 1, name: 'Laptop', assetCode: 'LAP-001', description: '', assetStatus: 'ACTIVE',
  category: cat1, images: null, attributeValues: null,
};

function makePage(assets: AssetResponse[] = [mockAsset], page = 0, total = 1): PagedModel<AssetResponse> {
  return {
    _embedded: { assets },
    page: { size: 12, totalElements: total, totalPages: Math.ceil(total / 12), number: page },
  };
}

function buildComponent() {
  const assetServiceMock = {
    getAll: vi.fn().mockReturnValue(of(makePage())),
    search: vi.fn().mockReturnValue(of(makePage())),
    filter: vi.fn().mockReturnValue(of(makePage())),
    getAllCategories: vi.fn().mockReturnValue(of([cat1, cat2, cat3])),
    getCategoryById: vi.fn().mockReturnValue(of({ ...cat2, attributes: [{ id: 9, name: 'RAM', dataType: 'NUMBER' }] })),
  } as unknown as AssetService;

  const authServiceMock = { isLoggedIn: signal(false) } as unknown as AuthService;

  const component = new HomeComponent(assetServiceMock, authServiceMock);
  return { component, assetServiceMock };
}

// ── Tests ──────────────────────────────────────────────────────────────────

describe('HomeComponent', () => {
  afterEach(() => vi.clearAllMocks());

  // ── ngOnInit ──────────────────────────────────────────────────────────

  it('loads categories and assets on init', () => {
    const { component, assetServiceMock } = buildComponent();
    component.ngOnInit();
    expect(assetServiceMock.getAllCategories).toHaveBeenCalledTimes(1);
    expect(assetServiceMock.getAll).toHaveBeenCalledTimes(1);
    expect(component.assets()).toEqual([mockAsset]);
    expect(component.loading()).toBe(false);
  });

  it('sets error state when getAll fails', () => {
    const { component, assetServiceMock } = buildComponent();
    (assetServiceMock.getAll as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new Error('network')),
    );
    component.ngOnInit();
    expect(component.error()).toContain('Failed to load assets');
    expect(component.loading()).toBe(false);
  });

  // ── loadAssets ────────────────────────────────────────────────────────

  it('updates pagination signals after loadAssets', () => {
    const { component, assetServiceMock } = buildComponent();
    (assetServiceMock.getAll as ReturnType<typeof vi.fn>).mockReturnValue(
      of(makePage([mockAsset], 0, 36)),
    );
    component.ngOnInit();
    expect(component.totalElements()).toBe(36);
    expect(component.totalPages()).toBe(3);
    expect(component.currentPage()).toBe(0);
  });

  // ── clearSearch ───────────────────────────────────────────────────────

  it('clearSearch resets query and reloads all assets', () => {
    const { component, assetServiceMock } = buildComponent();
    component.ngOnInit();
    component.searchQuery = 'laptop';
    component.clearSearch();
    expect(component.searchQuery).toBe('');
    // getAll called on init + again on clearSearch
    expect(assetServiceMock.getAll).toHaveBeenCalledTimes(2);
  });

  // ── applyFilters ──────────────────────────────────────────────────────

  it('calls filter service when a non-empty filter is applied', () => {
    const { component, assetServiceMock } = buildComponent();
    component.ngOnInit();
    component.filterName = 'Laptop';
    component.applyFilters();
    expect(assetServiceMock.filter).toHaveBeenCalledTimes(1);
  });

  it('falls back to getAll when the filter form is empty', () => {
    const { component, assetServiceMock } = buildComponent();
    component.ngOnInit();
    component.applyFilters(); // all fields are empty → mode = 'all'
    expect(assetServiceMock.getAll).toHaveBeenCalledTimes(2); // init + fallback
    expect(assetServiceMock.filter).not.toHaveBeenCalled();
  });

  // ── clearFilters ──────────────────────────────────────────────────────

  it('clearFilters resets all filter fields and reloads', () => {
    const { component, assetServiceMock } = buildComponent();
    component.ngOnInit();
    component.filterName = 'X';
    component.filterAssetCode = 'Y';
    component.filterStatus = 'ACTIVE';
    component.clearFilters();
    expect(component.filterName).toBe('');
    expect(component.filterAssetCode).toBe('');
    expect(component.filterStatus).toBe('');
    expect(assetServiceMock.getAll).toHaveBeenCalledTimes(2);
  });

  // ── hasActiveFilters ──────────────────────────────────────────────────

  it('hasActiveFilters is true when filterName is set', () => {
    const { component } = buildComponent();
    component.filterName = 'something';
    expect(component.hasActiveFilters).toBe(true);
  });

  it('hasActiveFilters is false when no filter fields are set', () => {
    const { component } = buildComponent();
    expect(component.hasActiveFilters).toBe(false);
  });

  // ── onCategoryChange ──────────────────────────────────────────────────

  it('populates attributeFilterRows when a category is selected', () => {
    const { component, assetServiceMock } = buildComponent();
    component.ngOnInit();
    component.filterCategoryId = 2;
    component.onCategoryChange();
    expect(assetServiceMock.getCategoryById).toHaveBeenCalledWith(2);
    expect(component.attributeFilterRows.length).toBe(1);
    expect(component.attributeFilterRows[0].attribute.name).toBe('RAM');
  });

  it('clears attributeFilterRows when category selection is cleared', () => {
    const { component } = buildComponent();
    component.attributeFilterRows = [{ attribute: { id: 1, name: 'A', dataType: 'STRING' }, valueString: '', valueNumber: '', valueDate: '', valueBool: false }];
    component.filterCategoryId = '';
    component.onCategoryChange();
    expect(component.attributeFilterRows).toEqual([]);
  });

  // ── subCategoriesOf ───────────────────────────────────────────────────

  it('subCategoriesOf returns child categories for a given parent', () => {
    const { component } = buildComponent();
    component.ngOnInit();
    const subs = component.subCategoriesOf(1);
    expect(subs).toEqual([cat2]);
  });

  // ── standaloneCategories ──────────────────────────────────────────────

  it('standaloneCategories returns top-level categories with no children', () => {
    const { component } = buildComponent();
    component.ngOnInit();
    // cat1 has children (cat2), cat3 has none
    expect(component.standaloneCategories).toContain(cat3);
    expect(component.standaloneCategories).not.toContain(cat1);
  });

  // ── groupedParentCategories ───────────────────────────────────────────

  it('groupedParentCategories returns only categories that have children', () => {
    const { component } = buildComponent();
    component.ngOnInit();
    expect(component.groupedParentCategories).toContain(cat1);
    expect(component.groupedParentCategories).not.toContain(cat3);
  });

  // ── goToPage ──────────────────────────────────────────────────────────

  it('goToPage does nothing when page is out of bounds', () => {
    const { component, assetServiceMock } = buildComponent();
    component.ngOnInit();
    const callsBefore = (assetServiceMock.getAll as ReturnType<typeof vi.fn>).mock.calls.length;
    component.goToPage(-1);
    component.goToPage(99);
    expect(assetServiceMock.getAll).toHaveBeenCalledTimes(callsBefore);
  });

  // ── visiblePages ──────────────────────────────────────────────────────

  it('visiblePages returns all pages when total <= 7', () => {
    const { component, assetServiceMock } = buildComponent();
    (assetServiceMock.getAll as ReturnType<typeof vi.fn>).mockReturnValue(
      of(makePage([mockAsset], 0, 60)), // 5 pages
    );
    component.ngOnInit();
    expect(component.visiblePages).toEqual([0, 1, 2, 3, 4]);
  });

  it('visiblePages inserts ellipsis for large page counts', () => {
    const { component, assetServiceMock } = buildComponent();
    (assetServiceMock.getAll as ReturnType<typeof vi.fn>).mockReturnValue(
      of(makePage([mockAsset], 5, 120)), // 10 pages, cur = 5
    );
    component.ngOnInit();
    expect(component.visiblePages).toContain('...');
  });
});

