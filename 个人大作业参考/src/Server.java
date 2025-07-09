import javax.swing.*; // 导入用于图形界面的 Swing 库
import java.awt.*; // 导入用于布局和图形处理的 AWT 库
import java.awt.event.ActionEvent; // 导入用于处理动作事件的类
import java.awt.event.ActionListener; // 导入动作事件监听器接口
import java.awt.event.WindowAdapter; // 导入窗口适配器类，用于处理窗口事件
import java.awt.event.WindowEvent; // 导入窗口事件类
import java.io.*; // 导入用于输入输出的类
import java.net.ServerSocket; // 导入服务器套接字类
import java.net.Socket; // 导入客户端套接字类
//import java.net.SocketException; // 导入套接字异常类
import java.text.SimpleDateFormat; // 导入日期格式化类
import java.util.ArrayList; // 导入数组列表类
import java.util.Date; // 导入日期类
import java.util.HashMap; // 导入哈希地图类
import java.util.Map; // 导入地图接口

public class Server extends JFrame {
    private static final int PORT = 8080; 
    JTextArea jta; 
    private JPanel south; 
    private JLabel jb; 
    private JButton stopBtn; 
    private JButton startBtn; 
    private boolean isStart = false; 
    private ServerSocket serverSocket = null; 
    private ArrayList<HandleClient> client = new ArrayList<>(); 
    private int peopleOnline = 0; 
    private Map<String, String> users = new HashMap<>(); 

    public Server() {
        this.setTitle("服务器端"); // 设置窗口标题为“服务端”
        jta = new JTextArea(); // 初始化文本区域
        JScrollPane jsp = new JScrollPane(jta); // 将文本区域放入滚动面板
        jta.setEditable(false); // 设置文本区域不可编辑
        jta.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        south = new JPanel(); // 初始化南边面板
        jb = new JLabel(); // 初始化标签
        stopBtn = new JButton("终止服务器"); // 初始化停止服务器按钮
        startBtn = new JButton("启动服务器"); // 初始化启动服务器按钮
        south.add(jb); // 将标签添加到南边面板
        jb.setText("聊天室目前在线人数: " + peopleOnline + "   "); // 设置标签显示在线人数
        south.add(startBtn); // 将启动按钮添加到南边面板
        south.add(stopBtn); // 将停止按钮添加到南边面板
        this.add(jsp, BorderLayout.CENTER); // 将滚动面板添加到窗口中央
        this.add(south, BorderLayout.SOUTH); // 将南边面板添加到窗口南边
        JPanel north = new JPanel(); // 初始化北边面板
        JTextField inputField = new JTextField(20); // 初始化输入框，长度为 20
        JButton sendBtn = new JButton("发送控制台命令"); // 初始化发送命令按钮
        north.add(inputField); // 将输入框添加到北边面板
        north.add(sendBtn); // 将发送按钮添加到北边面板
        this.add(north, BorderLayout.NORTH); // 将北边面板添加到窗口北边

        // 为发送按钮添加动作监听器
        sendBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String command = inputField.getText(); // 获取输入的命令
                switch (command) {
                    case "list":
                        listOnlineUsers(); // 列出在线用户
                        break;
                    case "listall":
                        listAllUsers(); // 列出所有用户
                        break;
                    case "quit":
                        isStart = false; // 设置服务器停止标识为 false
                        try {
                            if (serverSocket != null) {
                                serverSocket.close(); // 关闭服务器套接字
                                isStart = false;
                                jta.append("服务器已关闭\n"); // 更新日志
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace(); // 打印异常信息
                        } finally {
                            System.exit(0); // 退出程序
                        }
                        break;
                    default:
                        // 处理无效命令的操作
                        jta.append("无效命令，请重新输入\n");
                        break;
                }
                inputField.setText(""); // 清空输入框
            }
        });

        this.setBounds(200, 100, 600, 600); // 设置窗口位置和大小
        this.setVisible(true); // 显示窗口
        loadUsers(); // 从文件加载用户数据

        startBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startServer(); // 启动服务器
            }
        });
        // 为停止按钮添加动作监听器
        stopBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isStart = false; // 设置服务器停止标识为 false
                try {
                    if (serverSocket != null) {
                        serverSocket.close(); // 关闭服务器套接字
                        isStart = false;
                        jta.append("服务器已关闭\n"); // 更新日志
                    }
                } catch (IOException ex) {
                    ex.printStackTrace(); // 打印异常信息
                }
            }
        });

        // 为窗口添加关闭事件监听器
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                isStart = false; // 设置服务器停止标识为 false
                try {
                    if (serverSocket != null) {
                        serverSocket.close(); // 关闭服务器套接字
                        isStart = false;
                        jta.append("服务器已关闭\n"); // 更新日志
                    }
                } catch (IOException ex) {
                    ex.printStackTrace(); // 打印异常信息
                } finally {
                    System.exit(0); // 退出程序
                }
            }
        });
    }

        // 从文件加载用户数据的方法
    private void loadUsers() {
        try (BufferedReader br = new BufferedReader(new FileReader("users.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    users.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            jta.append("用户文件读取错误\n");
        }
    }

    private synchronized void saveUser(String username, String password) {
            try (FileWriter fw = new FileWriter("users.txt", true);
                    BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(username + "," + password + "\n");
                bw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 列出在线用户的方法
    private void listOnlineUsers() {
        StringBuilder onlineUsers = new StringBuilder("在线用户：");
        int count = 0;
        for (HandleClient cc : client) {
            onlineUsers.append(cc.name).append(" ");
            count++;
        }
        appendToLog(onlineUsers.toString());
        appendToLog("当前在线人数: " + count + "人");
    }

    // 列出所有用户的方法
    private void listAllUsers() {
        StringBuilder allUsers = new StringBuilder("全部用户：");
        int count = 0;
        for (String user : users.keySet()) {
            allUsers.append(user).append(" ");
            count++;
        }
        appendToLog(allUsers.toString());
        appendToLog("系统总人数: " + count + "人");
    }


    // 将消息追加到日志的方法
    private void appendToLog(String message) {
        SwingUtilities.invokeLater(() -> jta.append(message + "\n"));
    }


    // 启动服务器的方法
    public void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT); // 创建服务器套接字
                isStart = true; // 设置服务器启动标识为 true
                jta.append("服务器已启动,等待客户端连接...\n");
                while (isStart) {
                    Socket socket = serverSocket.accept(); // 接受客户端连接
                    new Thread(new HandleClient(socket)).start(); // 启动新的线程处理客户端连接
                }
            } catch (IOException e) {
                e.printStackTrace(); // 打印异常信息
            }
        }).start(); // 启动线程
    }

    // 处理客户端连接的内部类
    class HandleClient implements Runnable {
        Socket socket = null; // 客户端套接字
        private String name; // 用户名
        private String anonymousName; // 匿名用户名
        private boolean isAnonymous = false; // 是否匿名标识
        private static final int MAX_MESSAGE_SIZE = 1024 * 1024 * 1024; // 10MB最大消息限制
        DataInputStream dis; // 数据输入流
        DataOutputStream dos; // 数据输出流

        public HandleClient(Socket socket) {
            this.socket = socket; // 初始化客户端套接字
            try {
                dis = new DataInputStream(socket.getInputStream()); // 初始化数据输入流
                dos = new DataOutputStream(socket.getOutputStream()); // 初始化数据输出流
            } catch (IOException e) {
                e.printStackTrace(); // 打印异常信息
            }
        }



        // 处理分块图片消息
        private String handleImageChunks(DataInputStream dis, int chunks) throws IOException {
            StringBuilder imageMessage = new StringBuilder();

            // 接收所有图片块
            for (int i = 0; i < chunks; i++) {
                String chunk = dis.readUTF();
                imageMessage.append(chunk);
            }

            // 等待结束标记
            String endMark = dis.readUTF();
            if (!endMark.equals("[IMG_END]")) {
                throw new IOException("图片传输异常：未收到结束标记");
            }

            return imageMessage.toString();
        }

        @Override
        public void run() {
            try {
                String ipAddress = socket.getInetAddress().getHostAddress(); // 获取客户端 IP 地址
                while (true) {
                    String credentials = dis.readUTF(); // 读取客户端发送的用户名和密码

                    // 检查消息大小
                    if (credentials.length() > MAX_MESSAGE_SIZE) {
                        System.out.println("警告：收到超大消息，已拒绝处理");
                        continue;
                    }

                    if (credentials.startsWith("REGISTER:")) { // 处理注册请求
                        String[] parts = credentials.substring(9).split(",");
                        if (parts.length == 2) {
                            String username = parts[0];
                            String password = parts[1];
                            if (users.containsKey(username)) {
                                dos.writeUTF("register_failed:用户名已存在");
                            } else {
                                // 添加新用户
                                users.put(username, password);
                                // 保存到文件
                                saveUser(username, password);
                                dos.writeUTF("register_success");
                                logToFile("新用户注册: 用户[" + username + "], IP地址: " + ipAddress + ", 时间: "
                                        + getCurrentTime() + "\n");
                            }
                            continue;
                        }
                    }
                    String[] parts = credentials.split(","); // 分割用户名和密码
                    if (parts.length == 2) {
                        String username = parts[0];
                        String password = parts[1];
                        if (users.containsKey(username) && users.get(username).equals(password)) {
                            dos.writeUTF("success"); // 发送成功消息给客户端
                            name = username; // 设置用户名
                            anonymousName = "匿名用户" + (int) (Math.random() * 10000); // 生成随机匿名用户名
                            synchronized (this) {
                                String loginMessage = "客户端[" + name + "]加入了聊天室\n";
                                jta.append(loginMessage); // 更新日志
                                broadcast(name + " 加入了聊天室\n"); // 广播用户加入消息
                                jb.setText("聊天室在线人数: " + (++peopleOnline) + "   "); // 更新在线人数标签
                                client.add(this); // 添加到客户端连接列表
                                logToFile("登录成功: 用户[" + name + "], IP地址: " + ipAddress + ", 时间: " + getCurrentTime()
                                        + "\n"); // 记录登录日志
                            }
                            handleClientCommunication(); // 处理客户端通信
                            break;
                        } else {
                            dos.writeUTF("fail"); // 发送失败消息给客户端
                            logToFile("登录失败: 用户[" + username + "], IP地址: " + ipAddress + ", 时间: " + getCurrentTime()
                                    + "\n"); // 记录登录失败日志
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace(); // 打印异常信息
            }
        }

        // 处理客户端通信的方法
        private void handleClientCommunication() throws IOException {
            String clientName = name; // 获取客户端用户名
            while (isStart) {
                String str = dis.readUTF(); // 读取客户端发送的信息

                // 处理分块图片消息
                if (str.startsWith("[IMG_START]")) {
                    int chunks = Integer.parseInt(str.substring(11));
                    str = handleImageChunks(dis, chunks);
                }

                if (str.equals("##exit")) {
                    String exitMessage = "客户端[" + clientName + "]下线了\n";
                    jta.append(exitMessage); // 更新日志
                    broadcast(clientName + " 退出了聊天室\n"); // 广播用户退出消息
                    jb.setText("聊天室在线人数: " + (--peopleOnline) + "   "); // 更新在线人数标签
                    client.remove(this); // 从客户端连接列表中移除
                    logToFile("用户退出: 用户[" + clientName + "], 时间: " + getCurrentTime() + "\n"); // 记录用户退出日志
                    break;
                } else if (str.equals("@@list")) {
                    listOnlineUsersForClient(); // 列出在线用户给客户端
                } else if (str.equals("@@quit")) {
                    String quitMessage = "客户端[" + clientName + "]请求退出聊天室\n";
                    jta.append(quitMessage); // 更新日志
                    broadcast(clientName + " 请求退出聊天室\n"); // 广播用户请求退出消息
                    jb.setText("聊天室在线人数: " + (--peopleOnline) + "   "); // 更新在线人数标签
                    client.remove(this); // 从客户端连接列表中移除
                    logToFile("用户退出: 用户[" + clientName + "], 时间: " + getCurrentTime() + "\n"); // 记录用户退出日志
                    dos.writeUTF("##exit"); // 发送退出消息给客户端
                    socket.close(); // 关闭客户端套接字
                    break;
                } else if (str.equals("@@anonymous")) {
                    isAnonymous = !isAnonymous; // 切换匿名状态
                    dos.writeUTF("已切换到 " + (isAnonymous ? "匿名" : "实名") + " 聊天模式"); // 发送切换模式消息给客户端
                    String modeMessage = "客户端[" + clientName + "]已切换到 " + (isAnonymous ? "匿名" : "实名") + " 聊天模式\n";
                    jta.append(modeMessage); // 更新日志
                    logToFile(modeMessage);
                    dos.flush();
                } else if (str.startsWith("@")) {
                    String[] parts = str.substring(1).split("：", 2); // 分割私聊消息
                    if (parts.length == 2) {
                        String receiver = parts[0];
                        String message = parts[1];
                        int colonIndex = message.indexOf("：");
                        String actualContent = colonIndex != -1 ? message.substring(colonIndex + 1) : message;
                        if (actualContent.trim().startsWith("[IMG]")) {
                            String imagemessage = "[" + name + "]向[" + receiver + "]发送了一张图片\n";
                            jta.append(imagemessage); // 更新日志
                            for (HandleClient handler : client) {
                                if (handler.name.equals(receiver)) {
                                    sendImageData(actualContent, handler.dos);
                                    break;
                                }
                            }
                        } else {
                            handlePrivateMessage(message, name, receiver); // 处理私聊消息
                        }
                    } else {
                        jta.append("无效的私聊格式: " + str + "\n");
                    }
                } else {
                    String displayName = isAnonymous ? anonymousName : name; // 获取显示名称
                    String message = "[" + displayName + "]：" + str + "\n";
                    if (str.startsWith("[IMG]")) {
                        String imagemessage = "[" + displayName + "]：发送了一张图片\n";
                        jta.append(imagemessage); // 更新日志
                        logToFile(imagemessage);

                    } else {
                        jta.append(message); // 更新日志
                        logToFile(message);
                    }
                    broadcast(message); // 广播消息
                }
            }
        }

        // 列出在线用户给客户端的方法
        private void listOnlineUsersForClient() {
            StringBuilder onlineUsers = new StringBuilder("在线用户：");
            synchronized (client) {
                for (HandleClient cc : client) {
                    onlineUsers.append(cc.name).append(" ");
                }
            }
            send(onlineUsers.toString()); // 发送在线用户列表给客户端
        }

        // 发送消息给客户端的方法
        public void send(String str) {
            try {
                dos.writeUTF(str); // 发送消息
                dos.flush();
            } catch (IOException e) {
                e.printStackTrace(); // 打印异常信息
            }
        }

        // 广播消息给所有客户端的方法
        public void broadcast(String str) {
            // 检查消息大小
            /*
             * if (str.length() > MAX_MESSAGE_SIZE) {
             * System.out.println("警告：消息大小超过限制，可能是非法的大型消息");
             * return;
             * }
             */

            synchronized (client) {
                ArrayList<HandleClient> toRemove = new ArrayList<>();
                for (HandleClient c : client) {
                    if (c.socket.isClosed() || !c.socket.isConnected()) {
                        toRemove.add(c);
                        continue;
                    }

                    try {

                        int colonIndex = str.indexOf("：");
                        String actualContent = colonIndex != -1 ? str.substring(colonIndex + 1) : str;
                        if (actualContent.trim().startsWith("[IMG]")) {
                            for (HandleClient handler : client) {
                                sendImageData(str, handler.dos);
                            }
                        } else {
                            c.dos.writeUTF(str);
                            c.dos.flush();
                        }
                    } catch (IOException e) {
                        System.err.println("发送消息失败: " + e.getMessage());
                        toRemove.add(c);
                    }
                }
                client.removeAll(toRemove);
            }
        }

        // 发送图片数据
        private void sendImageData(String message, DataOutputStream dos) throws IOException {
            String[] parts = message.split("\\|", 2);
            String header = parts[0] + "|"; // [IMG]username|
            String imageData = parts[1]; // base64数据

            // 计算分块
            int chunkSize = 8192; // 8KB per chunk
            int totalChunks = (int) Math.ceil(imageData.length() / (double) chunkSize);

            // 发送开始标记和总块数
            dos.writeUTF("[IMG_START]" + totalChunks);
            dos.flush();

            // 发送第一块（包含头部）
            int firstChunkDataSize = Math.min(chunkSize - header.length(), imageData.length());
            String firstChunk = header + imageData.substring(0, firstChunkDataSize);
            dos.writeUTF(firstChunk);
            dos.flush();

            // 发送剩余的数据块
            for (int i = firstChunkDataSize; i < imageData.length(); i += chunkSize) {
                int end = Math.min(i + chunkSize, imageData.length());
                String chunk = imageData.substring(i, end);
                dos.writeUTF(chunk);
                dos.flush();
            }

            // 发送结束标记
            dos.writeUTF("[IMG_END]");
            dos.flush();
        }

        // 处理私聊消息的方法
        private void handlePrivateMessage(String message, String sender, String receiver) {
            boolean userFound = false;
            synchronized (client) {
                for (HandleClient c : client) {
                    if (c.name.equals(receiver)) {
                        try {
                            jta.append("[" + sender + "] 对 [" + receiver + "]私聊: " + message + "\n");
                            c.dos.writeUTF("[" + sender + "] 私聊说: " + message + "\n"); // 发送私聊消息给接收者
                            c.dos.flush();
                            userFound = true;
                        } catch (IOException e) {
                            e.printStackTrace(); // 打印异常信息
                        }
                        break;
                    }
                }
            }
            if (!userFound) {
                try {
                    dos.writeUTF("用户 [" + receiver + "] 不在线/不存在/为匿名用户  \n"); // 发送用户不在线消息
                    dos.flush();
                } catch (IOException e) {
                    e.printStackTrace(); // 打印异常信息
                }
            }
        }

        // 将日志写入文件的方法
        private void logToFile(String message) {
            if (!message.trim().isEmpty()) {
                String timeStr = getCurrentTime();
                String logEntry = timeStr + " " + message + System.lineSeparator();

                // 确保日志文件存在
                File logFile = new File("journal.txt");
                try {
                    if (!logFile.exists()) {
                        logFile.createNewFile();
                    }

                    // 写入日志
                    try (FileWriter fw = new FileWriter(logFile, true);
                            BufferedWriter bw = new BufferedWriter(fw)) {
                        bw.write(logEntry);
                    }
                } catch (IOException e) {
                    System.out.println("写入日志失败：" + e.getMessage());
                }
            }
        }

        // 获取当前时间的方法
        private String getCurrentTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 定义日期格式
            return sdf.format(new Date()); // 返回格式化后的当前时间
        }
    }

    public static void main(String[] args) {
        new Server(); // 启动服务器
    }

}