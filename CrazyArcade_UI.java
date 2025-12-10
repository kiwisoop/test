import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.net.URL;
import java.io.File;
import javax.sound.sampled.*;

// ========================================================
// [핵심] BGM 플레이어 클래스 (Java 기본 라이브러리 - WAV 지원)
// ========================================================
class BGMPlayer {
    private static BGMPlayer instance;
    private Clip clip;
    private FloatControl volumeControl;
    private boolean initialized = false;

    private BGMPlayer() {
    }

    public static BGMPlayer getInstance() {
        if (instance == null) {
            instance = new BGMPlayer();
        }
        return instance;
    }

    public void loadAndPlay(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("BGM 파일을 찾을 수 없습니다: " + filePath);
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            clip = AudioSystem.getClip();
            clip.open(audioStream);

            // 볼륨 컨트롤 설정
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                setVolume(GameSettings.bgmVolume);
            }

            clip.loop(Clip.LOOP_CONTINUOUSLY); // 무한 반복
            clip.start();
            initialized = true;
            System.out.println("BGM 재생 시작: " + filePath);

        } catch (Exception e) {
            System.err.println("BGM 로드 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setVolume(int volume) {
        if (volumeControl != null) {
            // 0-100을 데시벨로 변환
            float min = volumeControl.getMinimum();
            float max = volumeControl.getMaximum();
            float gain = min + (max - min) * (volume / 100.0f);
            volumeControl.setValue(gain);
        }
    }

    public void stop() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    public void pause() {
        stop();
    }

    public void resume() {
        if (clip != null) {
            clip.start();
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}

// ========================================================
// [핵심] 공통 테마 색상 (바나나 테마)
// ========================================================
class ThemeColors {
    public static final Color BG = new Color(255, 250, 205); // 배경 (연한 크림색)
    public static final Color MAIN = new Color(255, 225, 53); // 메인 노랑 (바나나)
    public static final Color DARK = new Color(139, 69, 19); // 갈색 (초코/껍질)
    public static final Color HIGHLIGHT = new Color(255, 240, 150); // 밝은 노랑
    public static final Color ACCENT = new Color(255, 180, 0); // 진한 노랑 (강조)
    public static final Color TEXT = new Color(100, 50, 0); // 텍스트 갈색
}

// ========================================================
// [핵심] 게임 설정값 저장 클래스
// ========================================================
class GameSettings {
    public static int bgmVolume = 50;
    public static int sfxVolume = 50;
    public static int p1_Up = KeyEvent.VK_W;
    public static int p1_Down = KeyEvent.VK_S;
    public static int p1_Left = KeyEvent.VK_A;
    public static int p1_Right = KeyEvent.VK_D;
    public static int p1_Bomb = KeyEvent.VK_SHIFT; // 물풍선: Shift
    public static int p1_Item = KeyEvent.VK_CONTROL; // 아이템: Ctrl
    public static int p2_Up = KeyEvent.VK_UP;
    public static int p2_Down = KeyEvent.VK_DOWN;
    public static int p2_Left = KeyEvent.VK_LEFT;
    public static int p2_Right = KeyEvent.VK_RIGHT;
    public static int p2_Bomb = KeyEvent.VK_NUMPAD1; // 물풍선: NumPad 1
    public static int p2_Item = KeyEvent.VK_NUMPAD0; // 아이템: NumPad 0
}

public class CrazyArcade_UI extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainContainer;
    public static final String PANEL_SPLASH = "SPLASH";
    public static final String PANEL_MENU = "MENU";
    public static final String PANEL_LOBBY = "LOBBY";
    public static final String PANEL_GAME = "GAME";
    public static final String PANEL_GUIDE = "GUIDE";
    public static final String PANEL_CREDITS = "CREDITS";
    public static final String PANEL_SETTINGS = "SETTINGS";

    public CrazyArcade_UI() {
        setTitle("Water Bomb Man - UI Prototype");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        mainContainer.add(new SplashPanel(this), PANEL_SPLASH);
        mainContainer.add(new MenuPanel(this), PANEL_MENU);
        mainContainer.add(new LobbyPanel(this), PANEL_LOBBY);
        mainContainer.add(new GamePanelPlaceholder(this), PANEL_GAME);
        mainContainer.add(new GuidePanel(this), PANEL_GUIDE);
        mainContainer.add(new CreditsPanel(this), PANEL_CREDITS);
        mainContainer.add(new SettingsPanel(this), PANEL_SETTINGS);

        add(mainContainer);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        // 스플래시 화면 먼저 표시
        showPanel(PANEL_SPLASH);
    }

    public void showPanel(String panelName) {
        cardLayout.show(mainContainer, panelName);
        if (panelName.equals(PANEL_GAME))
            mainContainer.getComponent(3).requestFocusInWindow();
        CreditsPanel cp = (CreditsPanel) mainContainer.getComponent(5);
        if (panelName.equals(PANEL_CREDITS))
            cp.startScrolling();
        else
            cp.stopScrolling();
    }

    // BGM 재생 시작 (메뉴 화면으로 이동 시 호출)
    public void startBGM() {
        String bgmPath = System.getProperty("user.dir") + File.separator + "노래.wav";
        BGMPlayer.getInstance().loadAndPlay(bgmPath);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CrazyArcade_UI());
    }
}

// ========================================================
// 1. 메뉴 화면
// ========================================================
class MenuPanel extends JPanel {
    private static final int PANEL_WIDTH = 800;
    private static final int PANEL_HEIGHT = 600;
    private Image backgroundImage;
    private CrazyArcade_UI mainFrame;

    public MenuPanel(CrazyArcade_UI mainFrame) {
        this.mainFrame = mainFrame;
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setLayout(null);

        // [수정된 부분 시작] 클래스 로더를 이용한 이미지 로드
        URL startUrl = getClass().getResource("/res/start.png");
        if (startUrl != null) {
            ImageIcon icon = new ImageIcon(startUrl);
            backgroundImage = icon.getImage();
        } else {
            // 이미지를 찾지 못했을 때 오류 메시지 출력 (디버깅 용)
            System.err.println("MenuPanel: Failed to load resource at /res/start.png");
            // 배경 이미지를 null로 두어 paintComponent에서 대체 배경이 표시되게 함
            backgroundImage = null;
        }
        // [수정된 부분 끝]

        int buttonWidth = 130;
        int buttonHeight = 45;
        int gap = 15;
        int startY = 500;
        int startX = (PANEL_WIDTH - ((buttonWidth * 5) + (gap * 4))) / 2;

        add(createRoundedButton("Game Start", startX, startY, buttonWidth, buttonHeight,
                e -> mainFrame.showPanel(CrazyArcade_UI.PANEL_LOBBY)));
        add(createRoundedButton("Guide", startX + (buttonWidth + gap) * 1, startY, buttonWidth, buttonHeight,
                e -> mainFrame.showPanel(CrazyArcade_UI.PANEL_GUIDE)));
        add(createRoundedButton("Settings", startX + (buttonWidth + gap) * 2, startY, buttonWidth, buttonHeight,
                e -> mainFrame.showPanel(CrazyArcade_UI.PANEL_SETTINGS)));
        add(createRoundedButton("Credits", startX + (buttonWidth + gap) * 3, startY, buttonWidth, buttonHeight,
                e -> mainFrame.showPanel(CrazyArcade_UI.PANEL_CREDITS)));
        add(createRoundedButton("Exit", startX + (buttonWidth + gap) * 4, startY, buttonWidth, buttonHeight,
                e -> System.exit(0)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (backgroundImage != null)
            g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        else
            g2.fillRect(0, 0, getWidth(), getHeight());
    }

    private JButton createRoundedButton(String text, int x, int y, int width, int height, ActionListener action) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed())
                    g2.setColor(ThemeColors.ACCENT);
                else if (getModel().isRollover())
                    g2.setColor(ThemeColors.HIGHLIGHT);
                else
                    g2.setColor(ThemeColors.MAIN);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
                // 테두리
                g2.setColor(ThemeColors.DARK);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 25, 25);
                super.paintComponent(g);
            }
        };
        btn.setBounds(x, y, width, height);
        btn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        btn.setForeground(ThemeColors.DARK);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.addActionListener(action);
        return btn;
    }
}

// ========================================================
// 2. 설정 (Settings) 패널 - [바나나 테마 + 폰트 깨짐 해결]
// ========================================================
class SettingsPanel extends JPanel {
    private static final int PANEL_WIDTH = 800;
    private static final int PANEL_HEIGHT = 600;
    private CrazyArcade_UI mainFrame;

    // 바나나 테마 색상 정의
    private final Color COLOR_BG = new Color(255, 250, 205); // 배경 (연한 크림색)
    private final Color COLOR_MAIN = new Color(255, 225, 53); // 메인 노랑 (바나나)
    private final Color COLOR_DARK = new Color(139, 69, 19); // 갈색 (초코/껍질)
    private final Color COLOR_HIGHLIGHT = new Color(255, 240, 150);

    public SettingsPanel(CrazyArcade_UI mainFrame) {
        this.mainFrame = mainFrame;
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setLayout(new BorderLayout());
        setBackground(COLOR_BG);

        // 상단 타이틀
        JLabel titleLabel = new JLabel("Settings", SwingConstants.CENTER);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 45));
        titleLabel.setForeground(COLOR_DARK);
        titleLabel.setBorder(new EmptyBorder(25, 0, 25, 0));
        add(titleLabel, BorderLayout.NORTH);

        // 탭 패널 커스터마이징
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        tabbedPane.setForeground(COLOR_DARK);
        tabbedPane.setBackground(COLOR_MAIN);

        tabbedPane.addTab(" 사운드 (Sound) ", createSoundPanel());
        tabbedPane.addTab(" 조작키 (Controls) ", createKeyMappingPanel());

        add(tabbedPane, BorderLayout.CENTER);

        // 하단 버튼 패널
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(20, 0, 20, 0));

        // 폰트 깨짐 방지를 위해 폰트 명시
        JButton backBtn = createBananaButton("저장 후 돌아가기");
        backBtn.setFont(new Font("맑은 고딕", Font.BOLD, 20)); // 한글 폰트 강제 지정
        backBtn.setPreferredSize(new Dimension(250, 60));
        backBtn.addActionListener(e -> mainFrame.showPanel(CrazyArcade_UI.PANEL_MENU));

        bottomPanel.add(backBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // --- 사운드 패널 (바나나 슬라이더) ---
    private JPanel createSoundPanel() {
        JPanel panel = new JPanel(null);
        panel.setBackground(COLOR_BG);

        // 배경음
        JLabel bgmLabel = new JLabel("배경음 (BGM)", SwingConstants.LEFT);
        bgmLabel.setFont(new Font("맑은 고딕", Font.BOLD, 22));
        bgmLabel.setForeground(COLOR_DARK);
        bgmLabel.setBounds(150, 80, 200, 30);
        panel.add(bgmLabel);

        JSlider bgmSlider = createBananaSlider(GameSettings.bgmVolume);
        bgmSlider.setBounds(350, 70, 300, 60);
        bgmSlider.addChangeListener(e -> {
            GameSettings.bgmVolume = bgmSlider.getValue();
            BGMPlayer.getInstance().setVolume(bgmSlider.getValue());
        });
        panel.add(bgmSlider);

        // 효과음
        JLabel sfxLabel = new JLabel("효과음 (SFX)", SwingConstants.LEFT);
        sfxLabel.setFont(new Font("맑은 고딕", Font.BOLD, 22));
        sfxLabel.setForeground(COLOR_DARK);
        sfxLabel.setBounds(150, 180, 200, 30);
        panel.add(sfxLabel);

        JSlider sfxSlider = createBananaSlider(GameSettings.sfxVolume);
        sfxSlider.setBounds(350, 170, 300, 60);
        sfxSlider.addChangeListener(e -> GameSettings.sfxVolume = sfxSlider.getValue());
        panel.add(sfxSlider);

        return panel;
    }

    // [핵심] 바나나 스타일 슬라이더 UI
    private JSlider createBananaSlider(int value) {
        JSlider slider = new JSlider(0, 100, value);
        slider.setOpaque(false);
        slider.setUI(new BasicSliderUI(slider) {
            @Override
            public void paintTrack(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Rectangle t = trackRect;
                // 트랙 배경 (노란색)
                g2.setColor(new Color(255, 240, 180));
                g2.fillRoundRect(t.x, t.y + t.height / 3, t.width, t.height / 3, 15, 15);
                // 트랙 테두리 (갈색)
                g2.setColor(COLOR_DARK);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(t.x, t.y + t.height / 3, t.width, t.height / 3, 15, 15);
                // 채워진 부분 (진한 노랑)
                int fillWidth = (int) (t.width * ((double) slider.getValue() / slider.getMaximum()));
                g2.setColor(COLOR_MAIN);
                g2.fillRoundRect(t.x, t.y + t.height / 3 + 2, fillWidth, t.height / 3 - 4, 10, 10);
            }

            @Override
            public void paintThumb(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 손잡이 (바나나 단면 모양)
                g2.setColor(COLOR_MAIN);
                g2.fillOval(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
                g2.setColor(COLOR_DARK);
                g2.setStroke(new BasicStroke(3));
                g2.drawOval(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
            }

            @Override
            protected Dimension getThumbSize() {
                return new Dimension(24, 24);
            }
        });
        return slider;
    }

    // --- 키 맵핑 패널 (바나나 박스 스타일) ---
    private JPanel createKeyMappingPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 40, 0));
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));
        panel.setBackground(COLOR_BG);

        // Player 1 박스
        JPanel p1Panel = createPlayerBox("1p");
        addKeyConfigRow(p1Panel, "위 (Up)", GameSettings.p1_Up, key -> GameSettings.p1_Up = key);
        addKeyConfigRow(p1Panel, "아래 (Down)", GameSettings.p1_Down, key -> GameSettings.p1_Down = key);
        addKeyConfigRow(p1Panel, "왼쪽 (Left)", GameSettings.p1_Left, key -> GameSettings.p1_Left = key);
        addKeyConfigRow(p1Panel, "오른쪽 (Right)", GameSettings.p1_Right, key -> GameSettings.p1_Right = key);
        addKeyConfigRow(p1Panel, "물풍선 (Bomb)", GameSettings.p1_Bomb, key -> GameSettings.p1_Bomb = key);
        addKeyConfigRow(p1Panel, "아이템 (Item)", GameSettings.p1_Item, key -> GameSettings.p1_Item = key);

        // Player 2 박스
        JPanel p2Panel = createPlayerBox("2p");
        addKeyConfigRow(p2Panel, "위 (Up)", GameSettings.p2_Up, key -> GameSettings.p2_Up = key);
        addKeyConfigRow(p2Panel, "아래 (Down)", GameSettings.p2_Down, key -> GameSettings.p2_Down = key);
        addKeyConfigRow(p2Panel, "왼쪽 (Left)", GameSettings.p2_Left, key -> GameSettings.p2_Left = key);
        addKeyConfigRow(p2Panel, "오른쪽 (Right)", GameSettings.p2_Right, key -> GameSettings.p2_Right = key);
        addKeyConfigRow(p2Panel, "물풍선 (Bomb)", GameSettings.p2_Bomb, key -> GameSettings.p2_Bomb = key);
        addKeyConfigRow(p2Panel, "아이템 (Item)", GameSettings.p2_Item, key -> GameSettings.p2_Item = key);

        panel.add(p1Panel);
        panel.add(p2Panel);
        return panel;
    }

    private JPanel createPlayerBox(String title) {
        JPanel panel = new JPanel(new GridLayout(7, 2, 10, 15)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 배경 박스 (둥근 사각형)
                g2.setColor(new Color(255, 255, 240)); // 아주 연한 아이보리
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);
                // 테두리
                g2.setColor(COLOR_DARK);
                g2.setStroke(new BasicStroke(3));
                g2.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 40, 40);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        titleLabel.setForeground(COLOR_DARK);
        panel.add(titleLabel);
        panel.add(new JLabel(""));
        return panel;
    }

    private void addKeyConfigRow(JPanel parent, String labelText, int currentKey, KeyUpdateCallback callback) {
        JLabel label = new JLabel(labelText, SwingConstants.RIGHT);
        label.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        label.setForeground(new Color(100, 50, 0)); // 진한 갈색

        JButton btn = createBananaButton(KeyEvent.getKeyText(currentKey));
        btn.addActionListener(e -> {
            btn.setText("입력...");
            btn.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent k) {
                    int keyCode = k.getKeyCode();
                    callback.update(keyCode);
                    btn.setText(KeyEvent.getKeyText(keyCode));
                    btn.removeKeyListener(this);
                }
            });
        });

        parent.add(label);
        parent.add(btn);
    }

    // [핵심] 바나나 스타일 버튼 (노란 배경 + 갈색 테두리)
    private JButton createBananaButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 마우스 상태에 따른 색상 변화
                if (getModel().isPressed())
                    g2.setColor(new Color(230, 200, 40));
                else if (getModel().isRollover())
                    g2.setColor(COLOR_HIGHLIGHT);
                else
                    g2.setColor(COLOR_MAIN);

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

                g2.setColor(COLOR_DARK);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 20, 20);

                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        btn.setForeground(COLOR_DARK);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        return btn;
    }

    interface KeyUpdateCallback {
        void update(int keyCode);
    }
}

// ========================================================
// 3. 가이드 패널
// ========================================================
class GuidePanel extends JPanel {
    private static final int PANEL_WIDTH = 800;
    private static final int PANEL_HEIGHT = 600;
    private Image guideImage;
    private CrazyArcade_UI mainFrame;

    public GuidePanel(CrazyArcade_UI mainFrame) {
        this.mainFrame = mainFrame;
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setLayout(null);
        setBackground(ThemeColors.BG);

        // [수정된 부분 시작] 클래스 로더를 이용한 이미지 로드
        URL guideUrl = getClass().getResource("/res/game play.png");
        if (guideUrl != null) {
            ImageIcon icon = new ImageIcon(guideUrl);
            guideImage = icon.getImage();
        } else {
            // 이미지 로드 실패 시 디버깅 메시지 출력
            System.err.println("GuidePanel: Failed to load resource at /res/game play.png");
            guideImage = null; // paintComponent에서 실패 처리
        }
        // [수정된 부분 끝]

        JButton backBtn = createThemedButton("홈으로");
        backBtn.setBounds(300, 520, 200, 50);
        backBtn.addActionListener(e -> mainFrame.showPanel(CrazyArcade_UI.PANEL_MENU));
        add(backBtn);
    }

    private JButton createThemedButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed())
                    g2.setColor(ThemeColors.ACCENT);
                else if (getModel().isRollover())
                    g2.setColor(ThemeColors.HIGHLIGHT);
                else
                    g2.setColor(ThemeColors.MAIN);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(ThemeColors.DARK);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 20, 20);
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        btn.setForeground(ThemeColors.DARK);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        return btn;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (guideImage != null && guideImage.getWidth(null) > 0) {
            int imgWidth = guideImage.getWidth(null);
            int imgHeight = guideImage.getHeight(null);
            double scaleX = (double) getWidth() / imgWidth;
            double scaleY = (double) getHeight() / imgHeight;
            double scale = Math.min(scaleX, scaleY);
            int scaledWidth = (int) (imgWidth * scale);
            int scaledHeight = (int) (imgHeight * scale);
            int x = (getWidth() - scaledWidth) / 2;
            int y = (getHeight() - scaledHeight) / 2;
            g2.drawImage(guideImage, x, y, scaledWidth, scaledHeight, this);
        } else {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 30));
            g2.drawString("이미지를 찾을 수 없습니다: game play.png", 150, 300);
        }
    }
}

// ========================================================
// 4. 크레딧 패널
// ========================================================
class CreditsPanel extends JPanel {
    private static final int PANEL_WIDTH = 800;
    private static final int PANEL_HEIGHT = 600;
    private CrazyArcade_UI mainFrame;
    private Timer scrollTimer;
    private int scrollY;
    private JPanel textPanel;
    private JPanel scrollContainer;
    private Image backgroundImage;

    private String creditsText = "<html><center>"
            + "<h1>Water Bomb Man</h1><br><br>"
            + "<h2>[ 개발팀 ]</h2>"
            + "<p>총괄 디렉터: 장수호</p>"
            + "<p>메인 프로그래머: 홍길동</p>"
            + "<p>UI/UX 디자인: 김철수</p><br>"
            + "<h2>[ 아트 & 사운드 ]</h2>"
            + "<p>캐릭터 디자인: 서승하</p>"
            + "<p>배경 및 이펙트: 이영희</p>"
            + "<p>사운드 디자인: 박민수</p><br>"
            + "<h2>[ 스페셜 땡스 ]</h2>"
            + "<p>물풍선 아이디어: 최이삭</p>"
            + "<p>QA 테스터: 팀원 전원</p><br><br>"
            + "<h3>Thank You for Playing!</h3>"
            + "</center></html>";

    public CreditsPanel(CrazyArcade_UI mainFrame) {
        this.mainFrame = mainFrame;
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setLayout(null);

        // [수정된 부분 시작] 클래스 로더를 이용한 이미지 로드
        URL creditsUrl = getClass().getResource("/res/creditss.png");
        if (creditsUrl != null) {
            ImageIcon icon = new ImageIcon(creditsUrl);
            backgroundImage = icon.getImage();
        } else {
            // 이미지 로드 실패 시 디버깅 메시지 출력
            System.err.println("CreditsPanel: Failed to load resource at /res/creditss.png");
            backgroundImage = null; // paintComponent에서 실패 처리
        }
        // [수정된 부분 끝]

        int viewportHeight = 500;
        scrollContainer = new JPanel();
        scrollContainer.setLayout(null);
        scrollContainer.setBounds(0, 0, PANEL_WIDTH, viewportHeight);
        scrollContainer.setOpaque(false);
        add(scrollContainer);

        textPanel = new JPanel();
        textPanel.setLayout(new BorderLayout());
        textPanel.setOpaque(false);
        textPanel.setBounds(0, viewportHeight, PANEL_WIDTH, 1000);

        JLabel label = new JLabel(creditsText);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        textPanel.add(label, BorderLayout.NORTH);

        scrollContainer.add(textPanel);

        JButton backBtn = new JButton("홈으로") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed())
                    g2.setColor(ThemeColors.ACCENT);
                else if (getModel().isRollover())
                    g2.setColor(ThemeColors.HIGHLIGHT);
                else
                    g2.setColor(ThemeColors.MAIN);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(ThemeColors.DARK);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 20, 20);
                super.paintComponent(g);
            }
        };
        backBtn.setBounds(300, 520, 200, 50);
        backBtn.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        backBtn.setForeground(ThemeColors.DARK);
        backBtn.setFocusPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.addActionListener(e -> mainFrame.showPanel(CrazyArcade_UI.PANEL_MENU));
        add(backBtn);

        scrollY = viewportHeight;

        scrollTimer = new Timer(50, e -> {
            scrollY -= 2;
            textPanel.setLocation(0, scrollY);
            if (scrollY + textPanel.getHeight() < 0) {
                scrollY = viewportHeight;
            }
            repaint();
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    public void startScrolling() {
        scrollY = 500;
        textPanel.setLocation(0, scrollY);
        scrollTimer.start();
    }

    public void stopScrolling() {
        scrollTimer.stop();
    }
}

// ========================================================
// 5. 대기실 (Lobby) 화면
// ========================================================
class LobbyPanel extends JPanel {
    private CrazyArcade_UI mainFrame;
    private String selectedCharacter = "배찌"; // 기본 선택 캐릭터

    public LobbyPanel(CrazyArcade_UI mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(null);
        setBackground(ThemeColors.BG);

        JLabel titleLabel = new JLabel("게임 로비 / Game Lobby");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 30));
        titleLabel.setForeground(ThemeColors.DARK);
        titleLabel.setBounds(30, 20, 500, 40);
        add(titleLabel);

        JPanel charPanel = createPanel("캐릭터 선택", 30, 80, 250, 400);

        // 배찌 캐릭터 카드
        JPanel bazziCard = createCharacterCard("배찌", "/res/배찌.png");
        bazziCard.setBounds(15, 40, 220, 160);
        charPanel.add(bazziCard);

        // 다오 캐릭터 카드
        JPanel daoCard = createCharacterCard("다오", "/res/다오.png");
        daoCard.setBounds(15, 210, 220, 160);
        charPanel.add(daoCard);

        // 캐릭터 선택 상호 배타적 처리
        bazziCard.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                selectedCharacter = "배찌";
                bazziCard.setBorder(BorderFactory.createLineBorder(ThemeColors.ACCENT, 4));
                daoCard.setBorder(BorderFactory.createLineBorder(ThemeColors.DARK, 2));
            }
        });
        daoCard.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                selectedCharacter = "우니";
                daoCard.setBorder(BorderFactory.createLineBorder(ThemeColors.ACCENT, 4));
                bazziCard.setBorder(BorderFactory.createLineBorder(ThemeColors.DARK, 2));
            }
        });

        // 기본 선택: 배찌
        bazziCard.setBorder(BorderFactory.createLineBorder(ThemeColors.ACCENT, 4));

        add(charPanel);

        JPanel mapPanel = createPanel("맵 정보", 300, 80, 450, 200);
        JLabel mapText = new JLabel("맵: 숲속마을 01");
        mapText.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        mapText.setForeground(ThemeColors.TEXT);
        mapText.setBounds(20, 80, 300, 30);
        mapPanel.add(mapText);
        add(mapPanel);

        JPanel chatPanel = createPanel("채팅", 300, 300, 450, 180);

        // 채팅 메시지 표시 영역
        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBackground(new Color(255, 255, 250));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBounds(10, 30, 430, 100);
        chatScroll.setBorder(BorderFactory.createLineBorder(ThemeColors.DARK, 1));
        chatPanel.add(chatScroll);

        // 입력 필드
        JTextField inputField = new JTextField();
        inputField.setBounds(10, 140, 350, 30);
        inputField.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        chatPanel.add(inputField);

        // 전송 버튼
        JButton sendBtn = createThemedButton("전송", 370, 140, 70, 30);
        sendBtn.addActionListener(e -> {
            String msg = inputField.getText().trim();
            if (!msg.isEmpty()) {
                chatArea.append(selectedCharacter + ": " + msg + "\n");
                inputField.setText("");
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            }
        });

        // 엔터키로도 전송
        inputField.addActionListener(e -> sendBtn.doClick());

        chatPanel.add(sendBtn);
        add(chatPanel);

        JButton backBtn = createThemedButton("뒤로", 30, 500, 150, 50);
        backBtn.addActionListener(e -> mainFrame.showPanel(CrazyArcade_UI.PANEL_MENU));
        add(backBtn);

        JButton startBtn = createStartButton("게임 시작!");
        startBtn.setBounds(600, 500, 150, 50);
        startBtn.addActionListener(e -> mainFrame.showPanel(CrazyArcade_UI.PANEL_GAME));
        add(startBtn);
    }

    private JButton createThemedButton(String text, int x, int y, int w, int h) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed())
                    g2.setColor(ThemeColors.ACCENT);
                else if (getModel().isRollover())
                    g2.setColor(ThemeColors.HIGHLIGHT);
                else
                    g2.setColor(ThemeColors.MAIN);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.setColor(ThemeColors.DARK);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 15, 15);
                super.paintComponent(g);
            }
        };
        btn.setBounds(x, y, w, h);
        btn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        btn.setForeground(ThemeColors.DARK);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        return btn;
    }

    private JButton createStartButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed())
                    g2.setColor(new Color(255, 120, 0));
                else if (getModel().isRollover())
                    g2.setColor(ThemeColors.ACCENT);
                else
                    g2.setColor(new Color(255, 160, 0));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(ThemeColors.DARK);
                g2.setStroke(new BasicStroke(3));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 20, 20);
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        btn.setForeground(ThemeColors.DARK);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        return btn;
    }

    private JPanel createCharacterCard(String name, String imagePath) {
        JPanel card = new JPanel() {
            private Image charImage;
            {
                URL imgUrl = getClass().getResource(imagePath);
                if (imgUrl != null) {
                    charImage = new ImageIcon(imgUrl).getImage();
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 배경
                g2.setColor(new Color(255, 255, 245));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                // 이미지 표시
                if (charImage != null) {
                    int imgSize = 100;
                    int x = (getWidth() - imgSize) / 2;
                    g2.drawImage(charImage, x, 10, imgSize, imgSize, this);
                }

                // 캐릭터 이름
                g2.setColor(ThemeColors.DARK);
                g2.setFont(new Font("맑은 고딕", Font.BOLD, 16));
                FontMetrics fm = g2.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(name)) / 2;
                g2.drawString(name, textX, getHeight() - 20);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createLineBorder(ThemeColors.DARK, 2));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return card;
    }

    private JPanel createPanel(String title, int x, int y, int w, int h) {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 245));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(ThemeColors.DARK);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 20, 20);
            }
        };
        p.setLayout(null);
        p.setBounds(x, y, w, h);
        p.setOpaque(false);
        JLabel l = new JLabel(title);
        l.setBounds(10, 8, 200, 20);
        l.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        l.setForeground(ThemeColors.DARK);
        p.add(l);
        return p;
    }
}

// ========================================================
// 6. 게임 패널 플레이스홀더
// ========================================================
class GamePanelPlaceholder extends JPanel {
    private CrazyArcade_UI mainFrame;

    public GamePanelPlaceholder(CrazyArcade_UI mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(null);
        setBackground(Color.BLACK);

        JLabel infoLabel = new JLabel("TEAM PROJECT: GAME LOGIC AREA");
        infoLabel.setForeground(Color.GREEN);
        infoLabel.setFont(new Font("Courier New", Font.BOLD, 30));
        infoLabel.setBounds(150, 200, 600, 50);
        add(infoLabel);

        JLabel subLabel = new JLabel("팀원들이 작성한 게임 패널 코드가 들어갈 자리입니다.");
        subLabel.setForeground(Color.WHITE);
        subLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 20));
        subLabel.setBounds(180, 260, 500, 30);
        add(subLabel);

        JLabel guideLabel = new JLabel("Press [ESC] to return to Lobby");
        guideLabel.setForeground(Color.YELLOW);
        guideLabel.setBounds(300, 400, 300, 30);
        add(guideLabel);

        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    mainFrame.showPanel(CrazyArcade_UI.PANEL_LOBBY);
                }
            }
        });
    }
}

// ========================================================
// 7. 스플래시 (Splash) 화면 - splash2.wav 효과음 재생
// ========================================================
class SplashPanel extends JPanel {
    private static final int PANEL_WIDTH = 800;
    private static final int PANEL_HEIGHT = 600;
    private CrazyArcade_UI mainFrame;
    private Timer transitionTimer;
    private float alpha = 0f; // 페이드 인 효과용
    private Timer fadeTimer;
    private Image splashImage;

    public SplashPanel(CrazyArcade_UI mainFrame) {
        this.mainFrame = mainFrame;
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setLayout(null);
        setBackground(Color.BLACK);

        // 스플래시 이미지 로드 시도 (res/splash.png)
        URL splashUrl = getClass().getResource("/res/splash.png");
        if (splashUrl != null) {
            splashImage = new ImageIcon(splashUrl).getImage();
        }

        // 마우스 클릭 시 바로 메뉴로 이동
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                goToMenu();
            }
        });

        // 키보드 입력 시 바로 메뉴로 이동
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                goToMenu();
            }
        });
    }

    // 패널이 화면에 표시될 때 호출
    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
        startSplash();
    }

    private void startSplash() {
        // splash2.wav 효과음 재생
        playSplashSound();

        // 페이드 인 효과
        alpha = 0f;
        fadeTimer = new Timer(30, e -> {
            alpha += 0.05f;
            if (alpha >= 1.0f) {
                alpha = 1.0f;
                fadeTimer.stop();
            }
            repaint();
        });
        fadeTimer.start();

        // 3초 후 메뉴 화면으로 자동 전환
        transitionTimer = new Timer(3000, e -> goToMenu());
        transitionTimer.setRepeats(false);
        transitionTimer.start();
    }

    private void playSplashSound() {
        try {
            String soundPath = System.getProperty("user.dir") + File.separator + "splash" + File.separator
                    + "splash2.wav";
            File soundFile = new File(soundPath);
            if (soundFile.exists()) {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);

                // SFX 볼륨 적용
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float min = volumeControl.getMinimum();
                    float max = volumeControl.getMaximum();
                    float gain = min + (max - min) * (GameSettings.sfxVolume / 100.0f);
                    volumeControl.setValue(gain);
                }

                clip.start();
                System.out.println("Splash 효과음 재생: " + soundPath);
            } else {
                System.err.println("Splash 효과음 파일을 찾을 수 없습니다: " + soundPath);
            }
        } catch (Exception e) {
            System.err.println("Splash 효과음 재생 실패: " + e.getMessage());
        }
    }

    private void goToMenu() {
        if (transitionTimer != null) {
            transitionTimer.stop();
        }
        if (fadeTimer != null) {
            fadeTimer.stop();
        }
        // BGM 재생 시작
        mainFrame.startBGM();
        // 메뉴 화면으로 이동
        mainFrame.showPanel(CrazyArcade_UI.PANEL_MENU);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 배경
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // 페이드 인 효과 적용
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        if (splashImage != null) {
            // 스플래시 이미지가 있으면 표시
            g2.drawImage(splashImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            // 이미지가 없으면 텍스트 로고 표시
            // 그라데이션 배경
            GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(255, 200, 50),
                    0, getHeight(), new Color(255, 100, 0));
            g2.setPaint(gradient);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // 게임 타이틀
            g2.setColor(ThemeColors.DARK);
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 60));
            String title = "Water Bomb Man";
            FontMetrics fm = g2.getFontMetrics();
            int titleX = (getWidth() - fm.stringWidth(title)) / 2;
            g2.drawString(title, titleX, 250);

            // 서브 타이틀
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 24));
            String subtitle = "물풍선 대작전!";
            fm = g2.getFontMetrics();
            int subX = (getWidth() - fm.stringWidth(subtitle)) / 2;
            g2.drawString(subtitle, subX, 310);
        }

        // 하단 안내 메시지
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        String hint = "클릭하거나 아무 키나 눌러서 시작";
        FontMetrics fm = g2.getFontMetrics();
        int hintX = (getWidth() - fm.stringWidth(hint)) / 2;
        g2.drawString(hint, hintX, getHeight() - 50);
        g2.dispose();
    }
}