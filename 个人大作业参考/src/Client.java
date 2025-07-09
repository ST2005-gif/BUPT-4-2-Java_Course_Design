import javax.swing.*; // Swing GUI组件
import javax.swing.text.*;
import java.awt.*; // AWT组件
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.util.Base64;
import java.net.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;

// 客户端主类，继承JFrame并实现KeyListener接口
public class Client extends JFrame implements KeyListener {

    private boolean isConn = false; // 标记是否连接到服务器
    private String name; // 客户端用户名
    private boolean isAnonymous = false; // 是否选择匿名聊天模式

    // GUI组件声明
    private JTextPane jta; // 用于显示聊天信息的文本区域

    // 添加显示消息的方法
    private void appendMessage(String message) {
        StyledDocument doc = jta.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), message + "\n", null);
            jta.setCaretPosition(doc.getLength()); // 自动滚动到底部
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    // 添加显示图片的方法
    private void appendImage(BufferedImage image) {
        StyledDocument doc = jta.getStyledDocument();
        Style style = jta.addStyle("Image", null);

        // 如果图片过大，进行缩放
        int maxWidth = 300; // 最大显示宽度
        int maxHeight = 300; // 最大显示高度

        int width = image.getWidth();
        int height = image.getHeight();

        if (width > maxWidth || height > maxHeight) {
            double scale = Math.min((double) maxWidth / width, (double) maxHeight / height);
            width = (int) (width * scale);
            height = (int) (height * scale);

            Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            ImageIcon icon = new ImageIcon(scaledImage);
            StyleConstants.setIcon(style, icon);
        } else {
            StyleConstants.setIcon(style, new ImageIcon(image));
        }

        try {
            doc.insertString(doc.getLength(), "invisible text", style);
            doc.insertString(doc.getLength(), "\n", null);
            jta.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private JScrollPane jsp; // 滚动面板，用于显示文本区域
    private JPanel jp; // 面板，用于放置输入文本框和发送按钮
    private JTextField jtf; // 输入消息的文本框
    private JTextField modeSelector;
    private JButton jb1; // 发送消息的按钮
    private JButton jbImage; // 发送图片按钮

    private Socket socket = null; // 套接字对象
    private static final String CONNSTR = "127.0.0.1"; // 连接服务器的IP地址
    private static final int CONNPORT = 8080; // 连接服务器的端口号
    private DataOutputStream dos = null; // 数据输出流

    // 客户端构造函数
    public Client() {
        jta = new JTextPane();
        jta.setEditable(false);
        jta.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        StyledDocument doc = jta.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_LEFT);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        jsp = new JScrollPane(jta);
        jp = new JPanel();
        jtf = new JTextField();
        modeSelector = new JTextField();
        jb1 = new JButton("发送信息");
        jbImage = new JButton("发送图片");
    }

    // 初始化方法，设置GUI界面和连接服务器
    public void init() {

        // 下部分放置输入框和按钮
        jp.setLayout(new FlowLayout());
        modeSelector.setPreferredSize(new Dimension(130, 25));
        jtf.setPreferredSize(new Dimension(240, 25));
        // modeSelector没有输入时，placeholder显示"匿名"
        jp.add(modeSelector);
        jp.add(jtf);
        jp.add(jb1);
        jp.add(jbImage);
        jtf.addKeyListener(this);
        jb1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == jb1) {
                    String text = jtf.getText();
                    String command = modeSelector.getText();
                    if (command.startsWith("@@")) {
                        handleSystemCommand(text);
                    } else {
                        sendMessage(text);
                    }
                    jtf.setText("");
                }
            }
        });

        jbImage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == jbImage) {
                    sendImage();
                }
            }
        });

        this.add(jsp, BorderLayout.CENTER); // 将滚动面板添加到窗口中间位置
        this.add(jp, BorderLayout.SOUTH); // 将面板添加到窗口底部位置

        this.setBounds(700, 300, 600, 600); // 设置窗口位置和大小
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 设置窗口关闭操作
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (dos != null) {
                        dos.writeUTF("##exit"); // 发送退出命令给服务器
                        dos.flush();
                    }
                    if (socket != null) {
                        socket.close(); // 关闭套接字
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                System.exit(0); // 退出程序
            }
        });

        try {
            socket = new Socket(CONNSTR, CONNPORT);
            isConn = true;

            // 创建登录界面组件
            JPanel loginPanel = new JPanel();
            loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));
            loginPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

            // 欢迎标题
            JLabel welcomeLabel = new JLabel("欢迎来到聊天室");
            welcomeLabel.setFont(new Font("微软雅黑", Font.BOLD, 24));
            welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // 创建输入面板
            JPanel inputPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            // 用户名输入框
            JLabel userLabel = new JLabel("用户名:");
            userLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            JTextField usernameField = new JTextField(20);
            usernameField.setPreferredSize(new Dimension(200, 30));

            // 密码输入框
            JLabel passLabel = new JLabel("密码:");
            passLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            JPasswordField passwordField = new JPasswordField(20);
            passwordField.setPreferredSize(new Dimension(200, 30));

            // 添加组件到输入面板
            gbc.gridx = 0;
            gbc.gridy = 0;
            inputPanel.add(userLabel, gbc);
            gbc.gridx = 1;
            inputPanel.add(usernameField, gbc);
            gbc.gridx = 0;
            gbc.gridy = 1;
            inputPanel.add(passLabel, gbc);
            gbc.gridx = 1;
            inputPanel.add(passwordField, gbc);

            // 按钮面板
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 0));

            JButton loginButton = new JButton("登录");
            JButton registerButton = new JButton("注册");

            // 设置按钮大小和样式
            Dimension buttonSize = new Dimension(100, 35);
            loginButton.setPreferredSize(buttonSize);
            registerButton.setPreferredSize(buttonSize);
            loginButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            registerButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));

            buttonPanel.add(loginButton);
            buttonPanel.add(registerButton);

            // 添加所有组件到主面板
            loginPanel.add(Box.createVerticalStrut(20));
            loginPanel.add(welcomeLabel);
            loginPanel.add(Box.createVerticalStrut(30));
            loginPanel.add(inputPanel);
            loginPanel.add(Box.createVerticalStrut(20));
            loginPanel.add(buttonPanel);

            // 设置登录界面
            JDialog loginDialog = new JDialog(this, "登录", true);
            loginDialog.setLayout(new BorderLayout());
            loginDialog.add(loginPanel, BorderLayout.CENTER);
            loginDialog.setSize(400, 300);
            loginDialog.setLocationRelativeTo(this);

            // 注册按钮事件
            registerButton.addActionListener(e -> {
                // 创建注册界面
                JPanel registerPanel = new JPanel();
                registerPanel.setLayout(new BoxLayout(registerPanel, BoxLayout.Y_AXIS));
                registerPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

                // 注册标题
                JLabel registerTitle = new JLabel("用户注册");
                registerTitle.setFont(new Font("微软雅黑", Font.BOLD, 24));
                registerTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

                // 注册输入面板
                JPanel regInputPanel = new JPanel(new GridBagLayout());
                GridBagConstraints regGbc = new GridBagConstraints();
                regGbc.fill = GridBagConstraints.HORIZONTAL;
                regGbc.insets = new Insets(5, 5, 5, 5);

                JLabel regUserLabel = new JLabel("新用户名:");
                regUserLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
                JTextField regUsernameField = new JTextField(20);
                regUsernameField.setPreferredSize(new Dimension(200, 30));

                JLabel regPassLabel = new JLabel("新密码:");
                regPassLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
                JPasswordField regPasswordField = new JPasswordField(20);
                regPasswordField.setPreferredSize(new Dimension(200, 30));

                regGbc.gridx = 0;
                regGbc.gridy = 0;
                regInputPanel.add(regUserLabel, regGbc);
                regGbc.gridx = 1;
                regInputPanel.add(regUsernameField, regGbc);
                regGbc.gridx = 0;
                regGbc.gridy = 1;
                regInputPanel.add(regPassLabel, regGbc);
                regGbc.gridx = 1;
                regInputPanel.add(regPasswordField, regGbc);

                JButton submitButton = new JButton("提交注册");
                submitButton.setPreferredSize(buttonSize);
                submitButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
                submitButton.setAlignmentX(Component.CENTER_ALIGNMENT);

                // 添加组件到注册面板
                registerPanel.add(Box.createVerticalStrut(20));
                registerPanel.add(registerTitle);
                registerPanel.add(Box.createVerticalStrut(30));
                registerPanel.add(regInputPanel);
                registerPanel.add(Box.createVerticalStrut(20));
                registerPanel.add(submitButton);

                JDialog registerDialog = new JDialog(loginDialog, "注册新用户", true);
                registerDialog.setLayout(new BorderLayout());
                registerDialog.add(registerPanel, BorderLayout.CENTER);
                registerDialog.setSize(400, 300);
                registerDialog.setLocationRelativeTo(loginDialog);

                submitButton.addActionListener(event -> {
                    String newUsername = regUsernameField.getText();
                    String newPassword = new String(regPasswordField.getPassword());

                    if (newUsername.isEmpty() || newPassword.isEmpty()) {
                        JOptionPane.showMessageDialog(registerDialog, "用户名和密码不能为空，请重新输入。");
                        return;
                    }

                    try {
                        dos = new DataOutputStream(socket.getOutputStream());
                        dos.writeUTF("REGISTER:" + newUsername + "," + newPassword);
                        dos.flush();

                        DataInputStream dis = new DataInputStream(socket.getInputStream());
                        String response = dis.readUTF();

                        if (response.equals("register_success")) {
                            JOptionPane.showMessageDialog(registerDialog, "注册成功！请返回登录。");
                            registerDialog.dispose();
                        } else {
                            JOptionPane.showMessageDialog(registerDialog, "注册失败：" + response);
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });

                registerDialog.setVisible(true);
            });

            // 登录按钮事件
            loginButton.addActionListener(e -> {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                if (username.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(loginDialog, "用户名和密码不能为空，请重新输入。");
                    return;
                }

                try {
                    dos = new DataOutputStream(socket.getOutputStream());
                    dos.writeUTF(username + "," + password);
                    dos.flush();

                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    String response = dis.readUTF();

                    if (response.equals("success")) {
                        name = username;
                        loginDialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(loginDialog, "用户名或密码错误，请重新输入。");
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

            loginDialog.setVisible(true);

            if (name == null) {
                System.exit(0);
            }

            this.setTitle("聊天室-" + name); // 设置窗口标题

            new Thread(new HandleServer()).start(); // 创建并启动接收消息的线程

            this.setVisible(true); // 设置窗口可见
            appendMessage("登录成功,欢迎来到聊天室，" + name + "!"); // 欢迎消息
            handleSystemCommand("list"); // 请求获取在线用户列表
        } catch (IOException e) {
            e.printStackTrace(); // 打印异常信息
        }
    }

    // 发送消息到服务器
    private void sendMessage(String str) {
        try {
            if (jtf.getText().length() == 0) {
                appendMessage("请输入消息内容!");
                return; // 如果文本输入框为空，直接返回
            }
            String mode = modeSelector.getText();
            if ((!mode.isEmpty() && !mode.startsWith("@")) || mode.equals("@")) {
                appendMessage("无效的命令格式,请重新输入");
                return;
            }
            if (mode.startsWith("@")) {
                str = mode + "：" + str;
            }

            if (str.startsWith("@")) { // 如果消息以"@"开头，表示私聊消息
                String[] parts = str.substring(1).split("：", 2); // 拆分消息，格式为@接收者：消息内容
                if (parts.length == 2) {
                    dos.writeUTF("@" + parts[0] + "：" + parts[1]); // 发送私聊消息给服务器
                    dos.flush(); // 刷新输出流
                } else {
                    appendMessage("无效的私聊格式: " + str); // 显示无效私聊格式提示
                }
            } else {
                dos.writeUTF(str); // 发送普通消息给服务器
                dos.flush(); // 刷新输出流
            }
        } catch (IOException e) {
            e.printStackTrace(); // 打印异常信息
        }
    }

    // 处理系统命令
    private void handleSystemCommand(String command) {
        try {
            command = "@@" + command;
            if (command.equals("@@list")) { // 如果命令是@@list，请求获取在线用户列表
                dos.writeUTF(command); // 发送命令给服务器
                dos.flush(); // 刷新输出流
            } else if (command.equals("@@quit")) { // 如果命令是@@quit，请求退出聊天室
                dos.writeUTF(command); // 发送命令给服务器
                dos.flush(); // 刷新输出流
                socket.close(); // 关闭套接字
                System.exit(0); // 退出程序
            } else if (command.equals("@@showanonymous")) { // 如果命令是@@showanonymous，显示当前聊天方式
                appendMessage("当前聊天方式为:" + (isAnonymous ? "匿名" : "实名")); // 显示当前聊天方式
            } else if (command.equals("@@anonymous")) { // 如果命令是@@anonymous，切换聊天方式
                isAnonymous = !isAnonymous; // 切换匿名聊天模式状态
                dos.writeUTF(command); // 发送命令给服务器
                dos.flush(); // 刷新输出流
                appendMessage("聊天方式已切换为:" + (isAnonymous ? "匿名" : "实名")); // 显示切换后的聊天方式
            } else {
                appendMessage("无效的系统命令，请重新输入"); // 显示无效系统命令提示
            }
        } catch (IOException e) {
            e.printStackTrace(); // 打印异常信息
        }
    }

    // 处理键盘按键按下事件
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            String text = jtf.getText().trim();
            String command = modeSelector.getText().trim();
            if (!text.isEmpty()) {
                if (command.startsWith("@@")) {
                    handleSystemCommand(text);
                } else {
                    sendMessage(text);
                }
                jtf.setText("");
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    // 内部类，用于接收服务器发送的消息

    private void handleReceivedMessage(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String timePrefix = "[" + sdf.format(new Date()) + "] ";
        int colonIndex = message.indexOf("：");
        String actualContent = colonIndex != -1 ? message.substring(colonIndex + 1) : message;
        if (actualContent.startsWith("[IMG]")) {
            // 处理图片消息
            try {
                String[] parts = actualContent.split("\\|");
                String sender = parts[0].substring(5); // 去掉[IMG]前缀
                appendMessage(timePrefix + sender + " 发送了一张图片:");

                // 清理Base64字符串
                String base64Image = parts[1].trim().replaceAll("\\s+", ""); // 移除所有空白字符

                // 解码Base64图片数据
                try {
                    byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
                    if (image != null) {
                        appendImage(image);
                    } else {
                        appendMessage(timePrefix + "图片数据无效");
                    }
                } catch (IllegalArgumentException e) {
                    appendMessage(timePrefix + "Base64解码失败: " + e.getMessage());
                }
            } catch (IOException | ArrayIndexOutOfBoundsException e) {
                appendMessage(timePrefix + "处理图片消息失败: " + e.getMessage());
            }
        } else {
            // 处理普通文本消息
            appendMessage(timePrefix + message);
        }
    }

    class HandleServer implements Runnable {
        private StringBuilder imageBuffer = new StringBuilder();
        private boolean isReceivingImage = false;
        private int expectedChunks = 0;
        private int receivedChunks = 0;

        @Override
        public void run() {
            try {
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                while (isConn) {
                    String message = dis.readUTF();

                    // 处理图片开始标记
                    if (message.startsWith("[IMG_START]")) {
                        isReceivingImage = true;
                        expectedChunks = Integer.parseInt(message.substring(11));
                        receivedChunks = 0;
                        imageBuffer = new StringBuilder();
                        continue;
                    }

                    // 处理图片结束标记
                    if (message.equals("[IMG_END]")) {
                        if (receivedChunks == expectedChunks) {
                            handleReceivedMessage(imageBuffer.toString());
                        } else {
                            System.err.println("接收到的图片数据不完整");
                        }
                        isReceivingImage = false;
                        continue;
                    }

                    // 接收图片数据块
                    if (isReceivingImage) {
                        imageBuffer.append(message);
                        receivedChunks++;
                        continue;
                    }

                    // 处理普通消息
                    handleReceivedMessage(message);
                }
            } catch (SocketException e) {
                appendMessage("服务器终止");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("图片文件", "jpg", "jpeg", "png", "gif"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                BufferedImage originalImage = ImageIO.read(file);

                // 压缩图片
                BufferedImage compressedImage = compressImage(originalImage);

                // 转换为Base64
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(compressedImage, "png", baos);
                byte[] imageBytes = baos.toByteArray();

                // 检查大小限制（5MB）
                if (imageBytes.length > 5 * 1024 * 1024) {
                    JOptionPane.showMessageDialog(this,
                            "图片太大，请选择更小的图片或降低图片质量",
                            "警告",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                String imageMessage = "[IMG]" + (isAnonymous ? "匿名用户" : name) + "|" + base64Image;
                String mode = modeSelector.getText();
                if (!mode.isEmpty() && !mode.startsWith("@")) {
                    appendMessage("无效的命令格式,请重新输入");
                }
                if (mode.startsWith("@")) {
                    imageMessage = mode + "：" + imageMessage;
                }

                // 分块发送图片数据
                int chunkSize = 60000; // 略小于65535以确保安全
                int length = imageMessage.length();
                int chunks = (length + chunkSize - 1) / chunkSize;

                // 发送图片块数
                dos.writeUTF("[IMG_START]" + chunks);
                dos.flush();

                // 分块发送图片数据
                for (int i = 0; i < chunks; i++) {
                    int start = i * chunkSize;
                    int end = Math.min(start + chunkSize, length);
                    String chunk = imageMessage.substring(start, end);
                    dos.writeUTF(chunk);
                    dos.flush();
                }

                dos.writeUTF("[IMG_END]");
                dos.flush();
                appendMessage("图片发送成功");

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "发送图片失败：" + ex.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private BufferedImage compressImage(BufferedImage image) {
        // 计算压缩后的尺寸
        int maxWidth = 1024; // 增加最大宽度
        int maxHeight = 768; // 增加最大高度
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();

        // 如果图片已经小于最大尺寸，无需压缩
        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return image;
        }

        // 计算缩放比例
        double scale = Math.min(
                (double) maxWidth / originalWidth,
                (double) maxHeight / originalHeight);

        // 计算新尺寸
        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        // 创建压缩后的图片，保持原始颜色模式
        BufferedImage compressedImage = new BufferedImage(
                newWidth,
                newHeight,
                image.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : image.getType());

        // 使用更高质量的缩放算法
        Graphics2D g = compressedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return compressedImage;
    }

    // 主方法，程序入口
    public static void main(String[] args) throws IOException {
        new Client().init(); // 创建客户端对象并初始化
    }
}