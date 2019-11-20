package tipl.util;

import tipl.formats.TImg;
import tipl.formats.TImgRO;

/**
 * This storage is still under-construction and current depends heavily on the virtualaim
 * implementation
 * Created by mader on 11/29/14.
 */
public class TIPLArrayBackedStorage extends TIPLVirtualAimStorage {
    //TODO enable this for testing
    @TIPLStorageManager.StorageInfo(storageType="ArrayBacked-Storage",desc="a storage " +
            "environment" +
            " using arrays of primitive arrays to store data in slices",enabled=false)

    final public static class tsStorage implements TIPLStorageManager.TIPLStorageFactory {
        @Override
        public ITIPLStorage get() {
            return new TIPLArrayBackedStorage();
        }
    };

    @Override
    public TImg wrapTImgRO(final TImgRO inImage) {
        if (inImage instanceof TImg) return (TImg) inImage;
        return TImg.ArrayBackedTImg.CreateFromTImg(inImage,inImage.getImageType());
    }

    @Override
    public TImgRO.FullReadable makeTImgFullReadable(TImgRO inImg) {
        return TImg.ArrayBackedTImg.CreateFromTImg(inImg, inImg.getImageType());
    }
}
