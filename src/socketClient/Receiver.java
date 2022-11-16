package socketClient;

import protocol.Protocol;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Receiver extends Thread implements Runnable {
    Socket socket = null;
    String id = null;
    String main = null;
    private int balance;
    public Receiver(Socket socket) {
        this.socket = socket;
    }

    public String[] fromString(String string) {
        String[] strings = string.replace("[", "").replace("]", "").split(", ");
        String result[] = new String[strings.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = strings[i];
        }
        return strings;
    }

    public void run() {
        try {
            // 서버로 보내는 스트림
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();
            Protocol protocol = new Protocol();
            byte[] buf = protocol.getPacket();

            // 콘솔창에 입력받기 위한 스트림
            InputStream in = System.in;
            InputStreamReader reader = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(reader);

            while (true) {
                inputStream.read(buf);  // 서버에서 받은 바이트를 buf에 저장
                int packetType = buf[0];
                protocol.setPacket(packetType, buf);  // buf의 값을 protocol에 복사
                if (packetType == Protocol.PT_EXIT) {
                    System.out.println("클라이언트 종료");
                    break;
                }
                switch (packetType) {
                    case Protocol.PT_UNDEFINED:
                        System.out.println("비정상적인 유저입니다.");
                        socket.close();
                        break;
                    case Protocol.PT_LOGIN_RES:
                        id = protocol.getId();
                        System.out.println(id + " " + "환영합니다! 메뉴를 골라주세요");
                        protocol = new Protocol(Protocol.PT_STOCK_REQ);
                        protocol.setId(id);
                        protocol.setClientType("1");
                        outputStream.write(protocol.getPacket());
                        break;
                    case Protocol.PT_LOGIN_REQ:
                        System.out.println("로그인을 해주세요");
                        System.out.print("ID를 입력하세요: ");
                        id = br.readLine();
                        protocol = new Protocol(Protocol.PT_LOGIN_RES);
                        protocol.setId(id);
                        protocol.setClientType("1");
                        outputStream.write(protocol.getPacket());
                        break;

                    case Protocol.PT_MAIN:
                        id = protocol.getId();
                        System.out.println("1. 주문하기");
                        System.out.println("2. 요청 보내기");
                        System.out.println("3. 요금 충전하기");
                        System.out.println("4. 끝내기");

                        main = br.readLine();

                        if (main.equals("1") || main.equals("3")) {
                            if (main.equals("3")){
                                String inputBalance = br.readLine();
                                balance = Integer.parseInt(inputBalance);
                            }
                            protocol = new Protocol(Protocol.PT_STOCK_REQ);
                            protocol.setId(id);
                            protocol.setClientType("1");
                            outputStream.write(protocol.getPacket());
                        } else if (main.equals("2")) {
                            break;
                        } else if (main.equals("4")) {
                            System.out.println("서비스를 종료합니다.");
                            break;
                        } else {
                            System.out.println("잘못된 입력입니다.");
                            break;
                        }
                        break;
                    case Protocol.PT_STOCK_RES:
                        id = protocol.getId();
                        System.out.println("[" + protocol.getId() + "님 환영합니다! 메뉴를 골라주세요!]");
                        System.out.println("<오늘의 메뉴>");
                        String[] menuList = fromString(protocol.getMenuName());
                        String[] amountList = fromString(protocol.getMenuAmount());
                        String[] priceList = fromString(protocol.getMenuPrice());

                        for (int i = 0; i < menuList.length; i++){
                            System.out.println((i + 1) + ". 메뉴:" + menuList[i] + " 남은 수량: " + amountList[i] + " 가격: " + priceList[i]);
                        }

                        // 메뉴 번호 입력 - 잘못입력시 while문 제대로 입력할 때까지
                        String menuName;
                        while (true){
                            System.out.print("주문할 메뉴의 번호를 입력하세요: ");
                            menuName = br.readLine();
                            // 메뉴번호 확인
                            if (0 >= Integer.parseInt(menuName) || menuList.length < Integer.parseInt(menuName)) {
                                System.out.println("잘못된 주문 번호입니다.");
                                continue;
                            }
                            break;
                        }

                        // 메뉴 수량 입력 - 잘못입력시 while문 제대로 입력할 때까지
                        String menuAmount;
                        while (true){
                            System.out.print("수량을 입력하세요 (100개 이하로 입력해주세요): ");
                            menuAmount = br.readLine();
                            // 수량 확인
                            if (100 < Integer.parseInt(menuAmount) || 0 >= Integer.parseInt(menuAmount)){
                                System.out.println("잘못된 수량 입니다.");
                                continue;
                            }
                            break;
                        }

                        protocol = new Protocol(Protocol.PT_ORDER);
                        protocol.setId(id);
                        protocol.setClientType("1");
                        protocol.setOrderFood(menuName);
                        protocol.setOrderAmount(menuAmount);
                        protocol.setOrderPrice(priceList[Integer.parseInt(menuName) - 1]);
                        outputStream.write(protocol.getPacket());
                        break;

                    case Protocol.PT_SHORTAGE_BALANCE:
                        // 잔액부족
                        System.out.println("잔액이 부족합니다.");
                        protocol = new Protocol(Protocol.PT_LOGIN_RES);
                        protocol.setId(id);
                        protocol.setClientType("1");
                        outputStream.write(protocol.getPacket());
                        break;

                    case Protocol.PT_SHORTAGE_STOCK:
                        // 수량부족
                        System.out.println("입력이 수량이 재고보다 많습니다.");
                        protocol = new Protocol(Protocol.PT_LOGIN_RES);
                        protocol.setId(id);
                        protocol.setClientType("1");
                        outputStream.write(protocol.getPacket());
                        break;

                    case Protocol.PT_ORDER_SUCCESS:
                        // 성공
                        balance = Integer.parseInt(protocol.getClientBalanceRes());
                        System.out.println("정상 처리 되었습니다.");
                        protocol = new Protocol(Protocol.PT_LOGIN_RES);
                        protocol.setId(id);
                        protocol.setClientType("1");
                        outputStream.write(protocol.getPacket());
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
