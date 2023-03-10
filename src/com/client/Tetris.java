package com.client;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.List;

import com.client.block.*;
import com.server.CountDownStart;
import com.server.GameOver;

public class Tetris extends Frame{

    public static boolean isGameOff;
    public static boolean isReady;
    public static boolean isWin;
    public static boolean isGameOn;
    public static int speedLevel;
    public static boolean isBeforeBlockStart;

    public static void envReset(MainGUI mainGUI) {
        Tetris.isGameOn = false;
        Tetris.isWin = true;
        Tetris.speedLevel = 0;
        Tetris.isReady = false;
        Tetris.isGameOff = false;
        Tetris.isBeforeBlockStart = true;
        mainGUI.startButton.setName("Ready");
    }

    public static void main(String[] args) {
        Tetris tetris = new Tetris();
        MainGUI mainGUI = new MainGUI();
//		mainGUI.requestFocus();
        mainGUI.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mainGUI.dispose();
            }
        });
        // 게임 준비 & 시작
        mainGUI.startButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Tetris.envReset(mainGUI);
                CountDown countDown = new CountDown(mainGUI);
                // 게임 스타트 및 타임 셋팅
                Thread gameStartThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // socket 연결
                        byte[] arr = {(byte)192,(byte)168,(byte)35,(byte)93};
//						byte[] arr = {(byte)192,(byte)168,(byte)240,67};
//						byte[] arr = {(byte)192,(byte)168,(byte)35,83};
                        InetAddress addr = null;
                        int port = 8080;
                        InputStream is = null;
                        ObjectInputStream ois = null;
                        OutputStream os= null;
                        ObjectOutputStream oos = null;
                        Socket socket = null;
                        Object object;
                        List<Block> blocks= new LinkedList<>();
                        try {
                            addr = InetAddress.getByAddress(arr);
                            socket = new Socket(addr, port);
                            os = socket.getOutputStream();
                            is = socket.getInputStream();
                            oos = new ObjectOutputStream(os);
                            ois = new ObjectInputStream(is);

                            do {
                                // 서버에 게임 준비 됨을 알림
                                if (!Tetris.isReady) {
                                    oos.writeObject(true);
                                    Tetris.isReady = true;
                                    oos.flush();
                                }

                                /*
                                 * 서버로부터 신호 받기 (객체로 구분)
                                 * CountDownStart : 게임시작전 카운트 다운
                                 * String : 현재까지 진행된 게임 시간
                                 * GameOver : 상대편 게임이 종료됨
                                 */
                                object = ois.readObject();
                                // 상대편이 먼저 게임 끝났을때 (승리 호출)
                                if(Tetris.isGameOn && object instanceof GameOver){
                                    Tetris.isGameOff=true;
                                    Tetris.isGameOn=false;
                                    if(Tetris.isWin){
                                        Tetris.showWinnerMark(mainGUI);
                                    }
                                    break;
                                }

                                // 게임 시작전 카운트 다운
                                if(Tetris.isReady && object instanceof CountDownStart){
                                    countDown.setCount(((CountDownStart) object).timeCount);
                                }

                                // 현재 까지 진행된 게임 시간
                                if (!Tetris.isGameOff && object instanceof String) {
                                    mainGUI.timeLabel.setText(object.toString());
                                    if (!Tetris.isGameOn) {
                                        Tetris.isGameOn = true;
                                    }
                                    oos.writeObject("게임중");
                                    oos.flush();
                                }

                                // 서버로부터 블럭 받기
                                if(object instanceof LinkedList) {
                                    blocks.addAll((LinkedList<?extends Block>) object);
                                }

                                // 게임용 쓰레드 시작
                                if(Tetris.isGameOn && Tetris.isBeforeBlockStart){
                                    Tetris.isBeforeBlockStart = false;
                                    mainGUI.requestFocusInWindow();
                                    Thread palyGameThread = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            tetris.playGame(mainGUI, blocks);
                                        }
                                    });
                                    palyGameThread.start();
                                }

                                // 먼저 게임 끝났음을 서버에 알림
                                if(Tetris.isGameOff){
                                    mainGUI.startButton.setName("Ready");
                                    oos.writeObject(new GameOver());
                                    oos.flush();
                                }


                            } while (true);

                        } catch (SocketException | EOFException e) {
                            Tetris.isGameOn = false;
                        } catch (IOException | ClassNotFoundException ex) {
                            ex.printStackTrace();
                        } finally {
                            try {
                                if(oos!=null)oos.close();
                                if(ois!=null)ois.close();
                                if(os!=null)os.close();
                                if(is!=null)is.close();
//								if(socket!=null)socket.close();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }

                    }
                });
                gameStartThread.start();

                // 게임 시작 -------------------------------------------
//				mainGUI.requestFocusInWindow();
//				tetris.playGame(mainGUI);

            }
        });

        mainGUI.endButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 게임 종료
                Tetris.isGameOn = false;
            }
        });

        mainGUI.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
				/*
				  블럭 모션

				  1. 블럭이 ""초 마다 한칸씩 아래로 떨어짐
				  2. 방향키 ↑ 클릭(38) : 시계 방향으로 블럭 회전
				  3. 방향키 → 클릭(39) : 오른쪽으로 한 칸 이동
				  4. 방향키 ← 클릭(37) : 왼쪽으로 한 칸 이동
				  5. 방향키 ↓ 클릭(40) : 아래로 한 칸 이동
				  6. space bar 클릭 (32) : 가장 아래로 이동
				 */
                if(Tetris.isGameOn) {
                    switch (e.getKeyCode()) {
                        case 32: mainGUI.block.pressSpaceKey(); break;
                        case 37: mainGUI.block.pressLeftKey(); break;
                        case 38: mainGUI.block.pressUpKey(); break;
                        case 39: mainGUI.block.pressRightKey(); break;
                        case 40: mainGUI.block.pressDownKey(); break;
                        default: break;
                    }
                }
            }
        });
    }

    // 이겼을 경우 화면에 승리 표현
    private static void showWinnerMark(MainGUI mainGUI) {
        for (int i = 3; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                mainGUI.playGroundLabel[i][j].setBackground(Color.white);
            }
        }

        for(int i = 4; i < 7; i++) {
            mainGUI.playGroundLabel[i][1].setBackground(Color.red);
            mainGUI.playGroundLabel[i][7].setBackground(Color.red);
            mainGUI.playGroundLabel[i][9].setBackground(Color.red);
        }
        mainGUI.playGroundLabel[7][2].setBackground(Color.red);
        mainGUI.playGroundLabel[6][3].setBackground(Color.red);
        mainGUI.playGroundLabel[5][4].setBackground(Color.red);
        mainGUI.playGroundLabel[6][5].setBackground(Color.red);
        mainGUI.playGroundLabel[7][6].setBackground(Color.red);
        mainGUI.playGroundLabel[8][9].setBackground(Color.red);
        mainGUI.startButton.setName("Ready");
    }


    //	------------------------------------------------------------------------------
    //	game play process
    //	------------------------------------------------------------------------------
    public void playGame(MainGUI mainGUI, List<Block> blocks) {

		/*
		  게임 시작

		  게임 화면에 카운트 다운 5 4 3 2 1
		  카운트 다운 후 블럭 스타트
		 */

		/*
		  블럭 가져오기

		  1. 서버로 부터 블럭 배열 받아오기
		  2. 받아온 블럭을 오래된 순서대로(queue 활용) 하나씩 꺼내서 next block 창에 배치
		  3. 게임 화면에서 블럭 동작 마감하면 next block 창에서 하나 꺼냄
		  4. 서버로 부터 몇개씩 가져오지? 한번에? 아니면 가져온 블럭이 일정 갯수 이하로 떨어지면 요청?
		 */
        Tetris.speedLevel = 5;
        for (int i = 0; i < blocks.size(); i++) {

            // next block 화면 초기화
            for (int k = 0; k < 10; k++) {
                for (int j = 0; j < 4; j++) {
                    mainGUI.topMenuLabel[k][j].setBackground(Color.white);
                }
            }

            // next block 화면 설정
            blocks.get(i+1).setNextBlock(1,mainGUI.topMenuLabel);
            blocks.get(i+2).setNextBlock(4,mainGUI.topMenuLabel);
            blocks.get(i+3).setNextBlock(7,mainGUI.topMenuLabel);

            // 화면에 블럭 출력
            mainGUI.block = blocks.get(i);
            mainGUI.block.setBlock(mainGUI.playGroundLabel);
            // score process
            checkLineComplete(mainGUI);
            if(!Tetris.isGameOn){
                break;
            }
        }
    }

    private void checkLineComplete(MainGUI mainGUI) {
        // scan
        boolean isComplete = true;
        List<Integer> completeLine = new ArrayList<>();
        for (int i = 19; i >=2; i--){
            for (int j = 0; j<10; j++){
                Color checkColor = mainGUI.playGroundLabel[i][j].getBackground();
                if(Objects.equals(checkColor, new Color(12, 6, 6)) || Objects.equals(checkColor, Color.white) ) {
                    isComplete = false;
                    break;
                }
            }
            if(isComplete){
                mainGUI.scoreLabel.setText(Integer.parseInt(mainGUI.scoreLabel.getText())+100+"");
                completeLine.add(i);
            }
            isComplete = true;
        }

        if(completeLine.size()==4){
            for (int i = completeLine.get(0); i >= 2; i--){
                for (int j = 0; j<10; j++){
                    if(i<=5){
                        mainGUI.playGroundLabel[i][j].setBackground(new Color(12, 6, 6));
                    } else {
                        mainGUI.playGroundLabel[i][j].setBackground(mainGUI.playGroundLabel[i-4][j].getBackground());
                    }
                }
            }
        } else if (completeLine.size()==1){
            for (int i = completeLine.get(0); i >= 2; i--){
                for (int j = 0; j<10; j++){
                    mainGUI.playGroundLabel[i][j].setBackground(mainGUI.playGroundLabel[i - 1][j].getBackground());
                }
            }
        } else if (completeLine.size() == 2) {
            for (int i = completeLine.get(0); i > completeLine.get(0)-2; i--){
                for (int j = 0; j<10; j++){
                    if(!completeLine.contains(i-1)){
                        mainGUI.playGroundLabel[i][j].setBackground(mainGUI.playGroundLabel[i-1][j].getBackground());
                    }else if(!completeLine.contains(i-2)){
                        mainGUI.playGroundLabel[i][j].setBackground(mainGUI.playGroundLabel[i-2][j].getBackground());
                    }
                }
            }
            for (int i = completeLine.get(0)-2; i >= 2; i--){
                for (int j = 0; j<10; j++){
                    mainGUI.playGroundLabel[i][j].setBackground(mainGUI.playGroundLabel[i-2][j].getBackground());
                }
            }
        } else if (completeLine.size() == 3){
            for (int i = completeLine.get(0); i > completeLine.get(0)-3; i--) {
                for (int j = 0; j < 10; j++) {
                    if (i <= 4) {
                        mainGUI.playGroundLabel[i][j].setBackground(new Color(12, 6, 6));
                    } else if (!completeLine.contains(i + 1)) {
                        mainGUI.playGroundLabel[i][j].setBackground(mainGUI.playGroundLabel[i - 1][j].getBackground());
                    } else if (!completeLine.contains(i - 2)) {
                        mainGUI.playGroundLabel[i][j].setBackground(mainGUI.playGroundLabel[i - 2][j].getBackground());
                    } else if (!completeLine.contains(i - 3)) {
                        mainGUI.playGroundLabel[i][j].setBackground(mainGUI.playGroundLabel[i - 3][j].getBackground());
                    }
                }
            }
            for (int i = completeLine.get(0)-3; i >= 2; i--){
                for (int j = 0; j<10; j++){
                    if(i<=4){
                        mainGUI.playGroundLabel[i][j].setBackground(new Color(12, 6, 6));
                    } else {
                        mainGUI.playGroundLabel[i][j].setBackground(mainGUI.playGroundLabel[i-3][j].getBackground());
                    }
                }
            }

        }

    }

}