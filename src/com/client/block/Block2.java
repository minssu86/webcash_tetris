package com.client.block;

import com.client.Tetris;

import java.awt.*;

public class Block2 extends Block {

    public Block2(){
        color = new Color(180, 93, 13);
        blockNum = new int[]{1,0,1,1,1,2,0,2};
        blockDirection = 1;
    }

    @Override
    public void setBlock(Label[][] playGroundArr) {
        this.playGroundArr = playGroundArr;
        a[0] = 1; a[1] = 3;
        b[0] = 1; b[1] = 4;
        c[0] = 1; c[1] = 5;
        d[0] = 0; d[1] = 5;
        changeColor();
        isStop = false;
        while (!isStop) {
            if(Tetris.isGameOff) {
                resetColor();
                break;
            }
            try {
                Thread.sleep(1000 - Tetris.speedLevel * 100L);
                pressDownKey();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void pressUpKey() {
        resetColor();
        switch (blockDirection%4){
            case 1: direction1(); break;
            case 2: direction2(); break;
            case 3: direction3(); break;
            case 0: direction0(); break;
        }
        changeColor();
        blockDirection++;
    }

    private void direction1() {
        a[0] -=1;
        a[1] +=1;
        c[0] +=1;
        c[1] -=1;
        d[0] +=2;
    }

    private void direction2() {
        a[0] +=1;
        a[1] +=1;
        c[0] -=1;
        c[1] -=1;
        d[1] -=2;
    }

    private void direction3() {
        a[0] +=1;
        a[1] -=1;
        c[0] -=1;
        c[1] +=1;
        d[0] -=2;
    }

    private void direction0() {
        a[0] -=1;
        a[1] -=1;
        c[0] +=1;
        c[1] +=1;
        d[1] +=2;
    }


}