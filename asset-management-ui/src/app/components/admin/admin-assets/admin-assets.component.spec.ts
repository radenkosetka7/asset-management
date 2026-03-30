import { describe, it, expect, vi, beforeEach } from 'vitest';
import { of } from 'rxjs';
import { Router } from '@angular/router';
import { AdminAssetsComponent } from './admin-assets.component';
import { AssetService } from '../../../services/asset.service';

function buildComponent() {
  const mockAsset = {
    id: 1,
    name: 'Asset',
    assetCode: 'AST-1',
    description: 'Desc',
    assetStatus: 'ACTIVE',
    category: { id: 1, name: 'Cat', description: '', parentCategoryId: null, attributes: [] },
    images: null,
    attributeValues: null,
  };

  const assetServiceMock = {
    getAll: vi.fn().mockReturnValue(of({
      _embedded: { assets: [] },
      page: { number: 0, totalPages: 1, totalElements: 0, size: 10 },
    })),
    getAllCategories: vi.fn().mockReturnValue(of([])),
    createAsset: vi.fn().mockReturnValue(of(mockAsset)),
    updateAsset: vi.fn().mockReturnValue(of(mockAsset)),
  } as unknown as AssetService;

  const routerMock = {
    navigate: vi.fn(),
  } as unknown as Router;

  const component = new AdminAssetsComponent(assetServiceMock, routerMock);
  return { component, assetServiceMock };
}

describe('AdminAssetsComponent image selection', () => {
  let component: AdminAssetsComponent;
  let assetServiceMock: AssetService;

  beforeEach(() => {
    const built = buildComponent();
    component = built.component;
    assetServiceMock = built.assetServiceMock;
  });

  it('appends files from repeated selections instead of replacing existing ones', () => {
    const first = new File(['1'], 'first.jpg', { type: 'image/jpeg', lastModified: 111 });
    const second = new File(['2'], 'second.jpg', { type: 'image/jpeg', lastModified: 222 });

    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [first], configurable: true });
    component.onFilesChange({ target: input } as unknown as Event);

    Object.defineProperty(input, 'files', { value: [second], configurable: true });
    component.onFilesChange({ target: input } as unknown as Event);

    expect(component.selectedFiles.map((f) => f.name)).toEqual(['first.jpg', 'second.jpg']);
  });

  it('does not duplicate the same file when selected again', () => {
    const duplicate = new File(['same'], 'same.jpg', { type: 'image/jpeg', lastModified: 111 });
    const input = document.createElement('input');

    Object.defineProperty(input, 'files', { value: [duplicate], configurable: true });
    component.onFilesChange({ target: input } as unknown as Event);

    Object.defineProperty(input, 'files', { value: [duplicate], configurable: true });
    component.onFilesChange({ target: input } as unknown as Event);

    expect(component.selectedFiles.length).toBe(1);
  });

  it('removes a selected file by index', () => {
    component.selectedFiles = [
      new File(['1'], 'one.jpg', { type: 'image/jpeg', lastModified: 1 }),
      new File(['2'], 'two.jpg', { type: 'image/jpeg', lastModified: 2 }),
    ];

    component.removeSelectedFile(0);

    expect(component.selectedFiles.map((f) => f.name)).toEqual(['two.jpg']);
  });

  it('clears all selected files', () => {
    component.selectedFiles = [new File(['1'], 'one.jpg', { type: 'image/jpeg', lastModified: 1 })];
    component.clearSelectedFiles();
    expect(component.selectedFiles).toEqual([]);
  });

  it('keeps at most 10 images and reports overflow', () => {
    const makeFile = (i: number) => new File([`${i}`], `img-${i}.jpg`, { type: 'image/jpeg', lastModified: i });
    const input = document.createElement('input');

    Object.defineProperty(input, 'files', { value: Array.from({ length: 8 }, (_, i) => makeFile(i + 1)), configurable: true });
    component.onFilesChange({ target: input } as unknown as Event);

    Object.defineProperty(input, 'files', { value: [makeFile(9), makeFile(10), makeFile(11), makeFile(12)], configurable: true });
    component.onFilesChange({ target: input } as unknown as Event);

    expect(component.selectedFiles.length).toBe(10);
    expect(component.imageLimitError()).toContain('Maximum 10 images allowed');
  });

  it('blocks save when image count is above max', () => {
    component.editingAsset.set(null);
    component.formAssetCode = 'AST-100';
    component.formName = 'Laptop';
    component.formDescription = 'Desc';
    component.formCategoryId = 1;
    component.selectedFiles = Array.from({ length: 11 }, (_, i) =>
      new File([`${i}`], `img-${i}.jpg`, { type: 'image/jpeg', lastModified: i }),
    );

    component.save();

    expect(component.modalError()).toContain('maximum of 10 images');
    expect((assetServiceMock.createAsset as ReturnType<typeof vi.fn>)).not.toHaveBeenCalled();
  });
});

