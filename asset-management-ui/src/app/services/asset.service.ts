import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AssetResponse, PagedModel, AssetFilterRequest, CategoryResponse,
  AssetRequest, CategoryRequest
} from '../models/asset.model';

@Injectable({
  providedIn: 'root'
})
export class AssetService {
  private readonly baseUrl = '/api/assets';
  private readonly categoryUrl = '/api/categories';

  constructor(private http: HttpClient) {}


  getAll(page = 0, size = 12, sort = 'id,asc'): Observable<PagedModel<AssetResponse>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', sort);
    return this.http.get<PagedModel<AssetResponse>>(this.baseUrl, { params });
  }

  getById(id: number): Observable<AssetResponse> {
    return this.http.get<AssetResponse>(`${this.baseUrl}/${id}`);
  }

  search(query: string, page = 0, size = 12): Observable<PagedModel<AssetResponse>> {
    const params = new HttpParams().set('query', query).set('page', page).set('size', size);
    return this.http.get<PagedModel<AssetResponse>>(`${this.baseUrl}/search`, { params });
  }

  filter(filterRequest: AssetFilterRequest, page = 0, size = 12): Observable<PagedModel<AssetResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.post<PagedModel<AssetResponse>>(`${this.baseUrl}/filter`, filterRequest, { params });
  }

  createAsset(assetRequest: AssetRequest, images?: File[]): Observable<AssetResponse> {
    const formData = new FormData();
    formData.append('asset', new Blob([JSON.stringify(assetRequest)], { type: 'application/json' }));
    if (images && images.length > 0) {
      images.forEach(f => formData.append('images', f));
    }
    return this.http.post<AssetResponse>(this.baseUrl, formData);
  }

  updateAsset(id: number, assetRequest: AssetRequest): Observable<AssetResponse> {
    return this.http.put<AssetResponse>(`${this.baseUrl}/${id}`, assetRequest);
  }

  deleteAsset(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  deleteAssetImage(assetId: number, imageId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/images/${assetId}/${imageId}`);
  }

  deleteAssetAttribute(assetId: number, attributeId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/attributes/${assetId}/${attributeId}`);
  }


  getAllCategories(): Observable<CategoryResponse[]> {
    return this.http.get<CategoryResponse[]>(this.categoryUrl);
  }

  getCategoryById(id: number): Observable<CategoryResponse> {
    return this.http.get<CategoryResponse>(`${this.categoryUrl}/${id}`);
  }

  createCategory(req: CategoryRequest): Observable<CategoryResponse> {
    return this.http.post<CategoryResponse>(this.categoryUrl, req);
  }

  deleteCategoryAttribute(categoryId: number, attributeId: number): Observable<void> {
    return this.http.delete<void>(`${this.categoryUrl}/attributes/${categoryId}/${attributeId}`);
  }

}
