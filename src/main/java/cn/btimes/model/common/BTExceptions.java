package cn.btimes.model.common;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/24/2018 11:22 PM
 */
public class BTExceptions {

    public static class PastDateException extends RuntimeException {
        private static final long serialVersionUID = 2346784065240518738L;

        public PastDateException() {
            super();
        }

        public PastDateException(String errorMsg) {
            super(errorMsg);
        }
    }

    public static class PageEndException extends RuntimeException {
        private static final long serialVersionUID = 2346784321240567438L;

        public PageEndException(String errorMsg) {
            super(errorMsg);
        }
    }

    public static class ArticleNoImageException extends RuntimeException {
        private static final long serialVersionUID = 2342184321240567438L;

        public ArticleNoImageException(String errorMsg) {
            super(errorMsg);
        }
    }
}
