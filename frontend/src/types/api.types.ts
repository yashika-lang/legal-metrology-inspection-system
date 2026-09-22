/**
 * Mirrors `com.legalmetrology.common.response.ApiResponse<T>` exactly —
 * every backend endpoint returns this envelope, success or failure.
 */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
  errors?: string[];
}

/** Mirrors `com.legalmetrology.common.response.PagedResponse<T>`. */
export interface PagedResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  last: boolean;
}
