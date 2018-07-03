package cn.edu.sjtu.iiot.system.batchqr.model;

/**
 * Created by zhyao on 18-2-4.
 */

public class Neighbor {
    public int from;
    public int to;
    public double simlarity;

    public Neighbor(int p1, int p2, double p3){
        this.from=p1;
        this.to=p2;
        this.simlarity=p3;
    }
}
