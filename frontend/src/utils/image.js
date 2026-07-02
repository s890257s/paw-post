/**
 * 圖片壓縮工具
 * 使用 Canvas 將圖片縮小以節省頻寬與儲存空間
 * 
 * @param {File} file 原始圖片檔案
 * @param {number} maxWidth 最大寬度限制
 * @param {number} quality 壓縮品質 0 到 1 之間
 * @returns {Promise<Blob>} 壓縮後的圖片 Blob
 */
export const compressImage = (file, maxWidth = 800, quality = 0.8) => {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        
        reader.onload = (event) => {
            const img = new Image();
            img.onload = () => {
                // 計算縮放比例
                let width = img.width;
                let height = img.height;
                
                if (width > maxWidth) {
                    height = Math.round((height * maxWidth) / width);
                    width = maxWidth;
                }

                // 使用 canvas 進行繪製與壓縮
                const canvas = document.createElement('canvas');
                canvas.width = width;
                canvas.height = height;
                const ctx = canvas.getContext('2d');
                
                // 繪製圖片
                ctx.drawImage(img, 0, 0, width, height);
                
                // 輸出 Blob
                canvas.toBlob(
                    (blob) => {
                        if (blob) {
                            resolve(blob);
                        } else {
                            reject(new Error('圖片壓縮失敗'));
                        }
                    },
                    'image/jpeg',
                    quality
                );
            };
            img.onerror = () => reject(new Error('載入圖片失敗'));
            img.src = event.target.result;
        };
        
        reader.onerror = () => reject(new Error('讀取檔案失敗'));
        reader.readAsDataURL(file);
    });
};
