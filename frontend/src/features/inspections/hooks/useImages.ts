import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { imagesApi } from "../api/imagesApi";
import { queryKeys } from "@/constants/queryKeys";
import type { ImageType } from "@/types/enums";

export function useInspectionImages(inspectionId: string) {
  return useQuery({
    queryKey: queryKeys.inspectionImages(inspectionId),
    queryFn: () => imagesApi.listByInspection(inspectionId),
    enabled: !!inspectionId,
  });
}

export function useUploadImage(inspectionId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ file, imageType }: { file: File; imageType?: ImageType }) =>
      imagesApi.upload(inspectionId, file, imageType),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.inspectionImages(inspectionId) });
    },
  });
}
