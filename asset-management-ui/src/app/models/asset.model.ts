export type AssetStatus = 'ACTIVE' | 'IN_USE' | 'DAMAGED' | 'RETIRED';
export type AttributeDataType = 'STRING' | 'NUMBER' | 'DATE' | 'BOOLEAN';

export interface AssetAttributeValueRequest {
  attributeId: number;
  valueString?: string | null;
  valueNumber?: number | null;
  valueData?: string | null;   // LocalDate as ISO string
  valueBool?: boolean | null;
}

export interface AssetRequest {
  assetCode: string;
  name: string;
  description: string;
  assetStatus?: AssetStatus | null;
  categoryId: number;
  assetAttributeValue?: AssetAttributeValueRequest[] | null;
}

export interface AttributeRequest {
  name: string;
  dataType: AttributeDataType;
}

export interface CategoryRequest {
  name: string;
  description: string;
  parentCategoryId?: number | null;
  attributeRequestList: AttributeRequest[];
}

export interface ImageResponse {
  id: number;
  fileName: string;
}

export interface CategoryResponse {
  id: number;
  name: string;
  description: string;
  parentCategoryId: number | null;
  attributes: AttributeResponse[] | null;
}

export interface AttributeResponse {
  id: number;
  name: string;
  dataType: string;
}

export interface AssetAttributeValueResponse {
  attribute: AttributeResponse;
  valueString: string | null;
  valueNumber: number | null;
  valueDate: string | null;
  valueBool: boolean | null;
}

export interface AssetResponse {
  id: number;
  name: string;
  assetCode: string;
  description: string;
  assetStatus: AssetStatus;
  category: CategoryResponse;
  images: ImageResponse[] | null;
  attributeValues: AssetAttributeValueResponse[] | null;
}

export interface AssetFilterRequest {
  assetCode?: string | null;
  name?: string | null;
  categoryName?: string | null;
  assetStatus?: AssetStatus | null;
  assetAttributes?: AssetAttributeValueResponse[] | null;
}

export interface PagedModel<T> {
  _embedded?: {
    assetResponseList?: T[];
    [key: string]: T[] | undefined;
  };
  page: {
    size: number;
    totalElements: number;
    totalPages: number;
    number: number;
  };
  _links: {
    self: { href: string };
    first?: { href: string };
    prev?: { href: string };
    next?: { href: string };
    last?: { href: string };
  };
}


