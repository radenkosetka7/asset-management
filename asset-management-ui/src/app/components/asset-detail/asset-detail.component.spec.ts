import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { of, throwError } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { AssetDetailComponent } from './asset-detail.component';
import { AssetService } from '../../services/asset.service';
import { AuthService } from '../../services/auth.service';
import { AssetResponse, ImageResponse, AssetAttributeValueResponse } from '../../models/asset.model';
import { signal } from '@angular/core';

// ── Fixtures ───────────────────────────────────────────────────────────────

const baseCategory = {
  id: 1, name: 'Electronics', description: '', parentCategoryId: null, attributes: [],
};

const mockAsset: AssetResponse = {
  id: 10,
  name: 'Laptop',
  assetCode: 'LAP-001',
  description: 'A laptop',
  assetStatus: 'ACTIVE',
  category: baseCategory,
  images: [{ id: 20, fileName: 'photo.jpg' }],
  attributeValues: [{
    attribute: { id: 5, name: 'RAM', dataType: 'NUMBER' },
    valueString: null, valueNumber: 16, valueDate: null, valueBool: null,
  }],
};

function buildComponent(assetId: string | null = '10', routerUrl = '/assets/10') {
  const routeMock = {
    snapshot: { paramMap: { get: vi.fn().mockReturnValue(assetId) } },
  } as unknown as ActivatedRoute;

  const routerMock = {
    navigate: vi.fn(),
    url: routerUrl,
  } as unknown as Router;

  const assetServiceMock = {
    getById: vi.fn().mockReturnValue(of(mockAsset)),
    deleteAssetImage: vi.fn().mockReturnValue(of(undefined)),
    deleteAssetAttribute: vi.fn().mockReturnValue(of(undefined)),
  } as unknown as AssetService;

  const authServiceMock = {
    isLoggedIn: signal(true),
  } as unknown as AuthService;

  const component = new AssetDetailComponent(routeMock, routerMock, assetServiceMock, authServiceMock);
  return { component, routeMock, routerMock, assetServiceMock, authServiceMock };
}

// ── Tests ──────────────────────────────────────────────────────────────────

describe('AssetDetailComponent', () => {
  afterEach(() => vi.clearAllMocks());

  // ── ngOnInit ──────────────────────────────────────────────────────────

  it('loads the asset by id on init', () => {
    const { component, assetServiceMock } = buildComponent('10');
    component.ngOnInit();
    expect(assetServiceMock.getById).toHaveBeenCalledWith(10);
    expect(component.asset()).toEqual(mockAsset);
    expect(component.loading()).toBe(false);
  });

  it('sets an error when the id is invalid', () => {
    const { component } = buildComponent(null);
    component.ngOnInit();
    expect(component.error()).toBe('Invalid asset ID.');
    expect(component.loading()).toBe(false);
  });

  it('sets an error when getById fails', () => {
    const { component, assetServiceMock } = buildComponent('10');
    (assetServiceMock.getById as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new Error('Network error')),
    );
    component.ngOnInit();
    expect(component.error()).toBe('Failed to load asset details.');
    expect(component.loading()).toBe(false);
  });

  // ── statusClass / statusLabel ─────────────────────────────────────────

  it.each([
    ['ACTIVE',  'status-active',  'Active'],
    ['IN_USE',  'status-in-use',  'In Use'],
    ['DAMAGED', 'status-damaged', 'Damaged'],
    ['RETIRED', 'status-retired', 'Retired'],
  ] as const)('statusClass/statusLabel for %s', (status, expectedClass, expectedLabel) => {
    const { component, assetServiceMock } = buildComponent('10');
    (assetServiceMock.getById as ReturnType<typeof vi.fn>).mockReturnValue(
      of({ ...mockAsset, assetStatus: status }),
    );
    component.ngOnInit();
    expect(component.statusClass).toBe(expectedClass);
    expect(component.statusLabel).toBe(expectedLabel);
  });

  it('returns empty string for statusClass/statusLabel when no asset is loaded', () => {
    const { component } = buildComponent(null);
    component.ngOnInit();
    expect(component.statusClass).toBe('');
    expect(component.statusLabel).toBe('');
  });

  // ── imageUrl ──────────────────────────────────────────────────────────

  it('imageUrl returns the correct API path', () => {
    const { component } = buildComponent();
    expect(component.imageUrl('photo.jpg')).toBe('/api/assets/images/file/photo.jpg');
  });

  it('imageUrl encodes special characters in file names', () => {
    const { component } = buildComponent();
    expect(component.imageUrl('my photo #1.jpg')).toBe('/api/assets/images/file/my%20photo%20%231.jpg');
  });

  it('selectedImage returns the currently selected image', () => {
    const { component } = buildComponent();
    component.ngOnInit();
    expect(component.selectedImage()).toEqual({ id: 20, fileName: 'photo.jpg' });
  });

  it('marks image as broken and reports it through isImageBroken', () => {
    const { component } = buildComponent();
    component.markImageBroken(20);
    expect(component.isImageBroken(20)).toBe(true);
  });

  // ── getAttributeValue ─────────────────────────────────────────────────

  it.each([
    [{ attribute: { id: 1, name: 'A', dataType: 'STRING' }, valueString: 'hello', valueNumber: null, valueDate: null, valueBool: null }, 'hello'],
    [{ attribute: { id: 2, name: 'B', dataType: 'NUMBER' }, valueString: null, valueNumber: 42, valueDate: null, valueBool: null }, '42'],
    [{ attribute: { id: 3, name: 'C', dataType: 'DATE' }, valueString: null, valueNumber: null, valueDate: '2025-01-01', valueBool: null }, '2025-01-01'],
    [{ attribute: { id: 4, name: 'D', dataType: 'BOOLEAN' }, valueString: null, valueNumber: null, valueDate: null, valueBool: true }, 'Yes'],
    [{ attribute: { id: 5, name: 'E', dataType: 'BOOLEAN' }, valueString: null, valueNumber: null, valueDate: null, valueBool: false }, 'No'],
    [{ attribute: { id: 6, name: 'F', dataType: 'STRING' }, valueString: null, valueNumber: null, valueDate: null, valueBool: null }, '—'],
  ] as [AssetAttributeValueResponse, string][])('getAttributeValue returns %s', (attr, expected) => {
    const { component } = buildComponent();
    expect(component.getAttributeValue(attr)).toBe(expected);
  });

  // ── isAdminContext ────────────────────────────────────────────────────

  it('isAdminContext is true when URL starts with /admin', () => {
    const { component } = buildComponent('10', '/admin/assets/10');
    expect(component.isAdminContext).toBe(true);
  });

  it('isAdminContext is false when URL is a public route', () => {
    const { component } = buildComponent('10', '/assets/10');
    expect(component.isAdminContext).toBe(false);
  });

  // ── goBack ────────────────────────────────────────────────────────────

  it('navigates to /admin/assets when in admin context', () => {
    const { component, routerMock } = buildComponent('10', '/admin/assets/10');
    component.ngOnInit();
    component.goBack();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/admin/assets']);
  });

  it('navigates to / when in public context', () => {
    const { component, routerMock } = buildComponent('10', '/assets/10');
    component.ngOnInit();
    component.goBack();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/']);
  });

  // ── delete image ──────────────────────────────────────────────────────

  it('sets deletingImage state on confirmDeleteImage', () => {
    const { component } = buildComponent();
    component.ngOnInit();
    const img: ImageResponse = { id: 20, fileName: 'photo.jpg' };
    component.confirmDeleteImage(img);
    expect(component.deletingImage()).toEqual({ assetId: 10, imageId: 20, fileName: 'photo.jpg' });
  });

  it('clears deletingImage on cancelDeleteImage', () => {
    const { component } = buildComponent();
    component.ngOnInit();
    component.confirmDeleteImage({ id: 20, fileName: 'photo.jpg' });
    component.cancelDeleteImage();
    expect(component.deletingImage()).toBeNull();
  });

  it('calls deleteAssetImage and reloads on deleteImage success', () => {
    const { component, assetServiceMock } = buildComponent();
    component.ngOnInit();
    component.confirmDeleteImage({ id: 20, fileName: 'photo.jpg' });
    component.deleteImage();
    expect(assetServiceMock.deleteAssetImage).toHaveBeenCalledWith(10, 20);
    expect(component.deletingImage()).toBeNull();
    // getById called again for reload
    expect(assetServiceMock.getById).toHaveBeenCalledTimes(2);
  });

  it('sets error state when deleteAssetImage fails', () => {
    const { component, assetServiceMock } = buildComponent();
    component.ngOnInit();
    (assetServiceMock.deleteAssetImage as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new Error('fail')),
    );
    component.confirmDeleteImage({ id: 20, fileName: 'photo.jpg' });
    component.deleteImage();
    expect(component.error()).toBe('Failed to delete image.');
    expect(component.deletingImageBusy()).toBe(false);
  });

  // ── delete attribute ──────────────────────────────────────────────────

  it('sets deletingAttr state on confirmDeleteAttr', () => {
    const { component } = buildComponent();
    component.ngOnInit();
    const attr = mockAsset.attributeValues![0];
    component.confirmDeleteAttr(attr);
    expect(component.deletingAttr()).toEqual({ assetId: 10, attributeId: 5, attrName: 'RAM' });
  });

  it('clears deletingAttr on cancelDeleteAttr', () => {
    const { component } = buildComponent();
    component.ngOnInit();
    component.confirmDeleteAttr(mockAsset.attributeValues![0]);
    component.cancelDeleteAttr();
    expect(component.deletingAttr()).toBeNull();
  });

  it('calls deleteAssetAttribute and reloads on deleteAttr success', () => {
    const { component, assetServiceMock } = buildComponent();
    component.ngOnInit();
    component.confirmDeleteAttr(mockAsset.attributeValues![0]);
    component.deleteAttr();
    expect(assetServiceMock.deleteAssetAttribute).toHaveBeenCalledWith(10, 5);
    expect(component.deletingAttr()).toBeNull();
  });

  it('sets error state when deleteAssetAttribute fails', () => {
    const { component, assetServiceMock } = buildComponent();
    component.ngOnInit();
    (assetServiceMock.deleteAssetAttribute as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new Error('fail')),
    );
    component.confirmDeleteAttr(mockAsset.attributeValues![0]);
    component.deleteAttr();
    expect(component.error()).toBe('Failed to delete attribute value.');
  });
});

