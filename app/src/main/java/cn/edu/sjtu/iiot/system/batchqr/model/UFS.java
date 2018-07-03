package cn.edu.sjtu.iiot.system.batchqr.model;

import java.util.ArrayList;

/**
 * Created by SHIYONG on 2018/3/26.
 */

public class UFS {
    public int[] set;

    public UFS(int nb_elements){
        this.set = new int[nb_elements];
        for(int i=0; i<nb_elements; i++){
            set[i] = -1;
        }
    }

    public int find(int x){
        if(set[x] < 0){
            return x;
        }
        else {
            return this.find(set[x]);
        }
    }

    public void join(int x1,int x2){
        int r1 = find(x1);
        int r2 = find(x2);

        if(set[r2]<set[r1]){
            set[r1] = r2;
        }else {
            if(set[r2] == set[r1]){
                set[r1]--;
            }
            set[r2] = r1;
        }
    }
}
