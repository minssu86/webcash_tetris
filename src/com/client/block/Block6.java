package com.client.block;

import java.awt.*;

public class Block6 extends Block {

    public Block6(){
        color = new Color(251, 6, 6);
        blockNum = new int[]{1,0,1,1,0,1,0,2};
    }
    @Override
    public boolean setBlock(int speedLevel, Label[][] playGroundArr) {
        this.playGroundArr = playGroundArr;
        this.speedLevel = speedLevel;
        a[0] = 1; a[1] = 3;
        b[0] = 1; b[1] = 4;
        c[0] = 0; c[1] = 4;
        d[0] = 0; d[1] = 5;
        changeColor();
        isStop = false;
        while (!isStop) {
            try {
                Thread.sleep(1000 - this.speedLevel * 100L);
                pressDownKey();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return isGameOn;
    }

    @Override
    public void pressUpKey() {

    }
}