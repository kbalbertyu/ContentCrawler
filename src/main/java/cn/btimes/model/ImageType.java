package cn.btimes.model;

import com.amzass.utils.common.Exceptions.BusinessException;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-10 4:51 PM
 */
public enum  ImageType {
    JPEG("FFD8FF"),
    PNG("89504E47"),
    GIF("47494638"),
    TIFF("49492A00"),
    BMP("424D");

    private final String hex;

    ImageType(String hex) {
        this.hex = hex;
    }

    public static String getTypeByHex(String hex) {
        for (ImageType type : ImageType.values()) {
            if (type.hex.startsWith(hex)) {
                return type.name().toLowerCase();
            }
        }
        throw new BusinessException(String.format("Unknown file type from hex: %", hex));
    }
}
