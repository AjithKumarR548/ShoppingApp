
import java.nio.file.Path;
import javax.swing.*;

public class ShopApp {
    static void useNimbus() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            useNimbus();
            ShopService service = new ShopService(SampleData.catalog(),
                    new OrderRepository(Path.of("orders.csv")));
            JFrame frame = new JFrame("ShopEasy - Online Shopping App");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new ShopPanel(service));
            frame.setSize(1200, 720);
            frame.setMinimumSize(new java.awt.Dimension(1000, 600));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
