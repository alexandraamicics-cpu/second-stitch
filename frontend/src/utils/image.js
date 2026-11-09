// Shrinks photos in the browser before uploading.
// A phone photo can be 3–8 MB; at 1200px wide it's about 100–250 KB,
// which keeps the database small and makes the closet load faster.

/** New width and height that fit inside max × max, keeping the shape. Never enlarges. */
export function fitWithin(width, height, max) {
  const scale = Math.min(1, max / Math.max(width, height))
  return { width: Math.round(width * scale), height: Math.round(height * scale) }
}

/**
 * Returns a smaller JPEG version of the photo.
 * If anything goes wrong (or the result isn't smaller), the original file is used.
 */
export async function shrinkImage(file, max = 1200, quality = 0.82) {
  try {
    const bitmap = await createImageBitmap(file)
    const { width, height } = fitWithin(bitmap.width, bitmap.height, max)

    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const ctx = canvas.getContext('2d')
    ctx.fillStyle = '#ffffff' // JPEG has no transparency: give see-through PNGs a white background
    ctx.fillRect(0, 0, width, height)
    ctx.drawImage(bitmap, 0, 0, width, height)
    bitmap.close?.()

    const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', quality))
    if (!blob || blob.size >= file.size) return file

    const name = file.name.replace(/\.[^.]+$/, '') + '.jpg'
    return new File([blob], name, { type: 'image/jpeg' })
  } catch {
    return file
  }
}
