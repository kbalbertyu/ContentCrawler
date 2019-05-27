package cn.btimes.model.nlp;

import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-05-27 11:25 AM
 */
@Data
public class TagSimilarity {
    private int tag1;
    private int tag2;
    private double scoreSimilar;
    private double scoreLiteral;

    public TagSimilarity(int tag1, int tag2, double scoreSimilar, double scoreLiteral) {
        this.tag1 = tag1;
        this.tag2 = tag2;
        this.scoreSimilar = scoreSimilar;
        this.scoreLiteral = scoreLiteral;
    }
}
