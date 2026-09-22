/**
 * A real phone camera photo (often 8-12MP, 5-12MB) takes far longer to
 * upload than its content requires — OCR/Vision AI need the label text
 * legible, not the full sensor resolution. Found live: a realistic 10MB
 * test photo took 57 seconds end-to-end, and 56 of those seconds were the
 * backend's own upload to Supabase Storage over this machine's limited home
 * upload bandwidth — the same bottleneck applies to the phone→backend leg
 * on a slow mobile connection. Downscaling and re-encoding client-side
 * before the file ever leaves the browser shrinks it 10-20x on a typical
 * photo, cutting both legs proportionally, with no visible loss for text
 * recognition at 2000px on the long edge.
 *
 * Never blocks an upload if compression itself fails (an unsupported
 * codec, a browser without OffscreenCanvas support, etc.) — falls back to
 * the original file so a slow upload is always better than a broken one.
 */
export async function compressImageForUpload(file: File, maxDimension = 2000, quality = 0.85): Promise<File> {
  if (file.size < 1.5 * 1024 * 1024) return file;

  try {
    const bitmap = await createImageBitmap(file);
    const scale = Math.min(1, maxDimension / Math.max(bitmap.width, bitmap.height));
    const width = Math.round(bitmap.width * scale);
    const height = Math.round(bitmap.height * scale);

    const canvas = document.createElement("canvas");
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext("2d");
    if (!ctx) {
      bitmap.close();
      return file;
    }
    ctx.drawImage(bitmap, 0, 0, width, height);
    bitmap.close();

    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, "image/jpeg", quality));
    if (!blob || blob.size >= file.size) return file;

    const baseName = file.name.replace(/\.\w+$/, "") || "photo";
    return new File([blob], `${baseName}.jpg`, { type: "image/jpeg", lastModified: file.lastModified });
  } catch {
    return file;
  }
}
