import { describe, it, expect, vi, afterEach } from 'vitest';
import { Router } from '@angular/router';
import { AssetCardComponent } from './asset-card.component';
import { AssetResponse } from '../../models/asset.model';

// ── Fixture ────────────────────────────────────────────────────────────────

const baseCategory = {
  id: 1, name: 'Electronics', description: '', parentCategoryId: null, attributes: [],
};

function makeAsset(overrides: Partial<AssetResponse> = {}): AssetResponse {
  return {
    id: 1,
    name: 'Laptop',
    assetCode: 'LAP-001',
    description: 'A laptop',
    assetStatus: 'ACTIVE',
    category: baseCategory,
    images: [{ id: 10, fileName: 'laptop.jpg' }],
    attributeValues: null,
    ...overrides,
  };
}

function buildComponent(asset: AssetResponse) {
  const routerMock = { navigate: vi.fn() } as unknown as Router;
  const component = new AssetCardComponent(routerMock);
  component.asset = asset;
  return { component, routerMock };
}

// ── Tests ──────────────────────────────────────────────────────────────────

describe('AssetCardComponent', () => {
  afterEach(() => vi.clearAllMocks());

  // ── navigateToDetail ──────────────────────────────────────────────────

  it('navigates to /assets/:id when navigateToDetail is called', () => {
    const { component, routerMock } = buildComponent(makeAsset({ id: 42 }));
    component.navigateToDetail();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/assets', 42]);
  });

  // ── statusClass ───────────────────────────────────────────────────────

  it.each([
    ['ACTIVE',  'status-active'],
    ['IN_USE',  'status-in-use'],
    ['DAMAGED', 'status-damaged'],
    ['RETIRED', 'status-retired'],
  ] as const)('statusClass returns "%s" for status %s', (status, expected) => {
    const { component } = buildComponent(makeAsset({ assetStatus: status }));
    expect(component.statusClass).toBe(expected);
  });

  // ── statusLabel ───────────────────────────────────────────────────────

  it.each([
    ['ACTIVE',  'Active'],
    ['IN_USE',  'In Use'],
    ['DAMAGED', 'Damaged'],
    ['RETIRED', 'Retired'],
  ] as const)('statusLabel returns "%s" for status %s', (status, expected) => {
    const { component } = buildComponent(makeAsset({ assetStatus: status }));
    expect(component.statusLabel).toBe(expected);
  });

  // ── firstImage ────────────────────────────────────────────────────────

  it('firstImage returns the URL of the first image', () => {
    const { component } = buildComponent(makeAsset());
    expect(component.firstImage).toBe('/api/assets/images/file/laptop.jpg');
  });

  it('firstImage returns null when images array is empty', () => {
    const { component } = buildComponent(makeAsset({ images: [] }));
    expect(component.firstImage).toBeNull();
  });

  it('firstImage returns null when images is null', () => {
    const { component } = buildComponent(makeAsset({ images: null }));
    expect(component.firstImage).toBeNull();
  });

  it('firstImage encodes special characters in file names', () => {
    const { component } = buildComponent(makeAsset({ images: [{ id: 1, fileName: 'my photo #1.png' }] }));
    expect(component.firstImage).toBe('/api/assets/images/file/my%20photo%20%231.png');
  });

  it('firstImage returns null after image load error', () => {
    const { component } = buildComponent(makeAsset());
    component.onImageError();
    expect(component.firstImage).toBeNull();
  });

  it('resets image fallback state when asset input changes', () => {
    const { component } = buildComponent(makeAsset());
    component.onImageError();
    component.asset = makeAsset({ id: 2, images: [{ id: 11, fileName: 'new.jpg' }] });
    component.ngOnChanges({
      asset: {
        previousValue: makeAsset(),
        currentValue: component.asset,
        firstChange: false,
        isFirstChange: () => false,
      },
    });
    expect(component.firstImage).toBe('/api/assets/images/file/new.jpg');
  });
});

