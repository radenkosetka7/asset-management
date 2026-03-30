import { describe, it, expect, vi, beforeEach } from 'vitest';
import { HttpClient, HttpParams } from '@angular/common/http';
import { of } from 'rxjs';
import { AssetService } from './asset.service';
import {
  AssetResponse, PagedModel, AssetFilterRequest, CategoryResponse,
  AssetRequest, CategoryRequest,
} from '../models/asset.model';

const mockCategory: CategoryResponse = {
  id: 1,
  name: 'Electronics',
  description: 'Electronic devices',
  parentCategoryId: null,
  attributes: [],
};

const mockAsset: AssetResponse = {
  id: 42,
  name: 'Laptop',
  assetCode: 'LAP-001',
  description: 'Dell XPS 15',
  assetStatus: 'ACTIVE',
  category: mockCategory,
  images: [{ id: 1, fileName: 'laptop.jpg' }],
  attributeValues: null,
};

const mockPage: PagedModel<AssetResponse> = {
  _embedded: { assets: [mockAsset] },
  page: { size: 12, totalElements: 1, totalPages: 1, number: 0 },
  _links: { self: { href: '/api/assets?page=0&size=12' } },
};

describe('AssetService', () => {
  let service: AssetService;
  let http: { get: ReturnType<typeof vi.fn>; post: ReturnType<typeof vi.fn>; put: ReturnType<typeof vi.fn>; delete: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    http = { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() };
    service = new AssetService(http as unknown as HttpClient);
  });

  describe('getAll', () => {
    it('calls GET /api/assets with default pagination params', () => {
      http.get.mockReturnValue(of(mockPage));
      let result: PagedModel<AssetResponse> | undefined;
      service.getAll().subscribe(r => (result = r));
      expect(result).toEqual(mockPage);
      expect(http.get).toHaveBeenCalledWith('/api/assets', expect.objectContaining({ params: expect.any(HttpParams) }));
    });

    it('passes custom page/size/sort parameters', () => {
      http.get.mockReturnValue(of(mockPage));
      service.getAll(2, 6, 'name,desc').subscribe();
      const [, options] = http.get.mock.calls[0];
      const params: HttpParams = options.params;
      expect(params.get('page')).toBe('2');
      expect(params.get('size')).toBe('6');
      expect(params.get('sort')).toBe('name,desc');
    });
  });

  describe('getById', () => {
    it('calls GET /api/assets/:id and returns the asset', () => {
      http.get.mockReturnValue(of(mockAsset));
      let result: AssetResponse | undefined;
      service.getById(42).subscribe(r => (result = r));
      expect(http.get).toHaveBeenCalledWith('/api/assets/42');
      expect(result).toEqual(mockAsset);
    });
  });

  describe('search', () => {
    it('calls GET /api/assets/search with query param', () => {
      http.get.mockReturnValue(of(mockPage));
      service.search('laptop').subscribe();
      const [url, options] = http.get.mock.calls[0];
      expect(url).toBe('/api/assets/search');
      expect((options.params as HttpParams).get('query')).toBe('laptop');
    });
  });

  describe('filter', () => {
    it('calls POST /api/assets/filter with the filter body', () => {
      http.post.mockReturnValue(of(mockPage));
      const filterReq: AssetFilterRequest = { name: 'Laptop', assetStatus: 'ACTIVE' };
      service.filter(filterReq).subscribe();
      expect(http.post).toHaveBeenCalledWith(
        '/api/assets/filter',
        filterReq,
        expect.objectContaining({ params: expect.any(HttpParams) }),
      );
    });
  });

  describe('createAsset', () => {
    it('calls POST /api/assets with FormData', () => {
      http.post.mockReturnValue(of(mockAsset));
      const req: AssetRequest = { assetCode: 'X', name: 'Y', description: 'Z', categoryId: 1 };
      service.createAsset(req).subscribe();
      const [url, formData] = http.post.mock.calls[0];
      expect(url).toBe('/api/assets');
      expect(formData).toBeInstanceOf(FormData);
    });

    it('appends image files to FormData when provided', () => {
      http.post.mockReturnValue(of(mockAsset));
      const req: AssetRequest = { assetCode: 'X', name: 'Y', description: 'Z', categoryId: 1 };
      const file = new File(['data'], 'photo.jpg', { type: 'image/jpeg' });
      service.createAsset(req, [file]).subscribe();
      const [, formData] = http.post.mock.calls[0] as [string, FormData];
      expect(formData.getAll('images').length).toBe(1);
    });

    it('appends multiple image files using the same "images" form field', () => {
      http.post.mockReturnValue(of(mockAsset));
      const req: AssetRequest = { assetCode: 'X', name: 'Y', description: 'Z', categoryId: 1 };
      const fileA = new File(['a'], 'a.jpg', { type: 'image/jpeg' });
      const fileB = new File(['b'], 'b.jpg', { type: 'image/jpeg' });

      service.createAsset(req, [fileA, fileB]).subscribe();

      const [, formData] = http.post.mock.calls[0] as [string, FormData];
      const images = formData.getAll('images') as File[];
      expect(images.length).toBe(2);
      expect(images[0].name).toBe('a.jpg');
      expect(images[1].name).toBe('b.jpg');
    });
  });

  describe('updateAsset', () => {
    it('calls PUT /api/assets/:id with the asset body', () => {
      http.put.mockReturnValue(of(mockAsset));
      const req: AssetRequest = { assetCode: 'X', name: 'Y', description: 'Z', categoryId: 1 };
      service.updateAsset(42, req).subscribe();
      expect(http.put).toHaveBeenCalledWith('/api/assets/42', req);
    });
  });

  describe('deleteAsset', () => {
    it('calls DELETE /api/assets/:id', () => {
      http.delete.mockReturnValue(of(undefined));
      service.deleteAsset(42).subscribe();
      expect(http.delete).toHaveBeenCalledWith('/api/assets/42');
    });
  });

  describe('deleteAssetImage', () => {
    it('calls DELETE /api/assets/images/:assetId/:imageId', () => {
      http.delete.mockReturnValue(of(undefined));
      service.deleteAssetImage(10, 5).subscribe();
      expect(http.delete).toHaveBeenCalledWith('/api/assets/images/10/5');
    });
  });

  describe('deleteAssetAttribute', () => {
    it('calls DELETE /api/assets/attributes/:assetId/:attributeId', () => {
      http.delete.mockReturnValue(of(undefined));
      service.deleteAssetAttribute(10, 3).subscribe();
      expect(http.delete).toHaveBeenCalledWith('/api/assets/attributes/10/3');
    });
  });

  describe('getAllCategories', () => {
    it('calls GET /api/categories and returns a list', () => {
      http.get.mockReturnValue(of([mockCategory]));
      let result: CategoryResponse[] | undefined;
      service.getAllCategories().subscribe(r => (result = r));
      expect(http.get).toHaveBeenCalledWith('/api/categories');
      expect(result).toEqual([mockCategory]);
    });
  });

  describe('getCategoryById', () => {
    it('calls GET /api/categories/:id', () => {
      http.get.mockReturnValue(of(mockCategory));
      service.getCategoryById(1).subscribe();
      expect(http.get).toHaveBeenCalledWith('/api/categories/1');
    });
  });

  describe('createCategory', () => {
    it('calls POST /api/categories with the request body', () => {
      http.post.mockReturnValue(of(mockCategory));
      const req: CategoryRequest = { name: 'Electronics', description: 'Desc', attributeRequestList: [] };
      service.createCategory(req).subscribe();
      expect(http.post).toHaveBeenCalledWith('/api/categories', req);
    });
  });

  describe('deleteCategoryAttribute', () => {
    it('calls DELETE /api/categories/attributes/:catId/:attrId', () => {
      http.delete.mockReturnValue(of(undefined));
      service.deleteCategoryAttribute(2, 7).subscribe();
      expect(http.delete).toHaveBeenCalledWith('/api/categories/attributes/2/7');
    });
  });
});

