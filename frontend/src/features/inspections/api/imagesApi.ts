import { apiClient, unwrap } from "@/services/apiClient";
import type { ApiResponse } from "@/types/api.types";
import type { ImageResponse } from "../types";
import type { ImageType } from "@/types/enums";

export const imagesApi = {
  upload: (inspectionId: string, file: File, imageType?: ImageType) => {
    const form = new FormData();
    form.append("file", file);
    if (imageType) form.append("imageType", imageType);
    return unwrap(
      apiClient.post<ApiResponse<ImageResponse>>(`/inspections/${inspectionId}/images`, form, {
        headers: { "Content-Type": "multipart/form-data" },
      }),
    );
  },

  listByInspection: (inspectionId: string) =>
    unwrap(apiClient.get<ApiResponse<ImageResponse[]>>(`/inspections/${inspectionId}/images`)),

  delete: (imageId: string) => apiClient.delete(`/images/${imageId}`),
};
