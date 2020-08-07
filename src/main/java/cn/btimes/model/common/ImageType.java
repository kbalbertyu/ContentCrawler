package cn.btimes.model.common;

import com.amzass.utils.common.Exceptions.BusinessException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-10 4:51 PM
 */
public enum ImageType {
    JPEG("FFD8FF"),
    PNG("89504E47"),
    GIF("47494638"),
    TIFF("49492A00"),
    BMP("424D"),
    WEBP("524946461c");

    public static ImageType[] ALLOWED_TYPES = {JPEG, PNG, GIF};

    public static ImageType DEFAULT_TYPE = JPEG;

    private final String hex;

    ImageType(String hex) {
        this.hex = hex;
    }

    public static ImageType getTypeByHex(String hex) {
        for (ImageType type : ImageType.values()) {
            if (type.hex.startsWith(hex)) {
                return type;
            }
        }
        throw new BusinessException(String.format("Unknown file type from hex: %s", hex));
    }

    public static ImageType findByExt(String ext) {
        if (StringUtils.equalsIgnoreCase(ext, "jpg")) {
            return JPEG;
        }
        for (ImageType type : ImageType.values()) {
            if (StringUtils.equalsIgnoreCase(type.toExt(), ext)) {
                return type;
            }
        }

        return DEFAULT_TYPE;
    }

    public String toExt() {
        return this.name().toLowerCase();
    }

    public boolean allowed() {
        return ArrayUtils.contains(ALLOWED_TYPES, this);
    }
}
