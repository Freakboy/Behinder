package vip.youwe.sheller.ui.controller;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import jdk.nashorn.api.scripting.JSObject;
import org.json.JSONArray;
import org.json.JSONObject;
import vip.youwe.sheller.core.PluginTools;
import vip.youwe.sheller.core.ShellService;
import vip.youwe.sheller.dao.ShellManager;
import vip.youwe.sheller.utils.Utils;

import java.io.File;
import java.util.List;

public class PluginViewController {

    private ShellManager shellManager;
    @FXML
    private WebView pluginWebView;
    @FXML
    private Button installLocalBtn;
    @FXML
    private Button installNetBtn;
    @FXML
    private Accordion pluginFlowPane;
    @FXML
    private GridPane pluginDetailGridPane;
    @FXML
    private Label pluginNameLabel;
    @FXML
    private Label pluginAuthorLabel;
    @FXML
    private Label pluginLinkLabel;
    @FXML
    private Label pluginCommentLabel;
    @FXML
    private ImageView qrcodeImageView;
    private JSONObject shellEntity;
    private ShellService currentShellService;
    private List<Thread> workList;
    private Label statusLabel;

    public void init(ShellService shellService, List<Thread> workList, Label statusLabel, ShellManager shellManager) {
        this.currentShellService = shellService;
        this.shellEntity = shellService.getShellEntity();
        this.workList = workList;
        this.statusLabel = statusLabel;
        this.shellManager = shellManager;
        initPluginView();
    }

    private void initPluginView() {
        initPluginInstall();
        PluginTools pluginTools = new PluginTools(this.currentShellService, this.pluginWebView, this.statusLabel, this.workList);
        WebEngine webEngine = this.pluginWebView.getEngine();
        webEngine.getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {


            if (newState == Worker.State.SUCCEEDED) {
                JSObject win = (JSObject) webEngine.executeScript("window");
                win.setMember("PluginTools", pluginTools);
            }
        });
        try {
            loadPlugins();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.pluginDetailGridPane.setOpacity(0.0D);
    }


    private void loadPluginDetail(JSONObject pluginObj) {
        this.pluginNameLabel.setText(String.format(this.pluginNameLabel.getText(), pluginObj.getString("name"), pluginObj.getString("version")));
        this.pluginAuthorLabel.setText(String.format(this.pluginAuthorLabel.getText(), pluginObj.getString("author")));
        this.pluginLinkLabel.setText(String.format(this.pluginLinkLabel.getText(), pluginObj.getString("link")));
        this.pluginCommentLabel.setText(String.format(this.pluginCommentLabel.getText(), pluginObj.getString("comment")));
        String pathFormat = "file://%s/Plugins/%s/%s";
        try {
            String qrcodeFilePath = String.format(pathFormat, Utils.getSelfPath(), pluginObj.getString("name"), pluginObj.getString("qrcode"));
            this.qrcodeImageView.setImage(new Image(qrcodeFilePath));
        } catch (Exception e) {
            this.statusLabel.setText("插件开发者赞赏二维码加载失败");
            e.printStackTrace();
        }
    }


    private void loadPlugins() throws Exception {
        String scriptType = this.shellEntity.getString("type");
        JSONArray pluginList = this.shellManager.listPlugin(scriptType);
        for (int i = 0; i < pluginList.length(); i++) {
            JSONObject pluginObj = pluginList.getJSONObject(i);
            addPluginBox(pluginObj);
        }
    }


    private boolean checkPluginExist(JSONObject pluginObj) throws Exception {
        String pluginName = pluginObj.getString("name");
        String scriptType = pluginObj.getString("scriptType");
        if (this.shellManager.findPluginByName(scriptType, pluginName) != null) {
            return true;
        }
        return false;
    }


    private void showErrorMessage(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        Window window = alert.getDialogPane().getScene().getWindow();
        window.setOnCloseRequest(event -> window.hide());

        alert.setTitle(title);
        alert.setHeaderText("");
        alert.setContentText(msg);
        alert.show();
    }


    private void addPluginBox(JSONObject pluginObj) throws Exception {
        String pluginName = pluginObj.getString("name");
        String pluginCommnet = pluginObj.getString("comment");
        String pathFormat = "file://%s/Plugins/%s/%s";
        String entryFilePath = String.format(pathFormat, Utils.getSelfPath(), pluginName, pluginObj.getString("entryFile"));
        String iconFilePath = String.format(pathFormat, Utils.getSelfPath(), pluginName, pluginObj.getString("icon"));

        int type = 0;
        switch (pluginObj.getString("type")) {
            case "scan":
                type = 0;
                break;
            case "exploit":
                type = 1;
                break;
            case "tool":
                type = 2;
                break;
            case "other":
                type = 3;
                break;
            default:
        }
        FlowPane flowPane = (FlowPane) ((AnchorPane) ((TitledPane) this.pluginFlowPane.getPanes().get(type)).getContent()).getChildren().get(0);
        VBox box = new VBox();

        ImageView pluginIcon = new ImageView(new Image(iconFilePath));
        pluginIcon.setFitHeight(30.0D);
        pluginIcon.setPreserveRatio(true);
        Label pluginLabel = new Label(pluginName);

        box.getChildren().add(pluginIcon);
        box.getChildren().add(pluginLabel);
        box.setPadding(new Insets(5.0D));
        box.setAlignment(Pos.CENTER);
        Tooltip tip = new Tooltip();
        tip.setText(pluginCommnet);
        Tooltip.install(box, tip);
        box.setOnMouseClicked(e -> {
            try {
                this.pluginWebView.getEngine().load(entryFilePath);
                this.pluginDetailGridPane.setOpacity(1.0D);
                loadPluginDetail(pluginObj);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });

        box.setOnMouseEntered(e -> {
            VBox v = (VBox) e.getSource();
            v.setStyle("-fx-background-color:blue");
        });
        box.setOnMouseExited(e -> {
            VBox v = (VBox) e.getSource();
            v.setStyle("-fx-background-color:transparent");
        });
        flowPane.getChildren().add(box);
        this.pluginFlowPane.getPanes().get(type).setExpanded(true);
    }

    private void initPluginInstall() {
        this.installLocalBtn.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("请选择需要安装的插件包");
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All ZIP Files", "*.zip"));

            File pluginFile = fileChooser.showOpenDialog(this.pluginFlowPane.getScene().getWindow());
            try {
                JSONObject pluginEntity = Utils.parsePluginZip(pluginFile.getAbsolutePath());
                if (checkPluginExist(pluginEntity)) {
                    showErrorMessage("错误", "安装失败，插件已存在");
                    return;
                }
                addPluginBox(pluginEntity);
                this.shellManager.addPlugin(pluginEntity.getString("name"), pluginEntity.getString("version"), pluginEntity.getString("entryFile"), pluginEntity.getString("scriptType"), pluginEntity.getString("type"), pluginEntity.getInt("isGetShell"), pluginEntity.getString("icon"), pluginEntity.getString("author"), pluginEntity.getString("link"), pluginEntity.getString("qrcode"), pluginEntity.getString("comment"));
                this.statusLabel.setText("插件安装成功。");
            } catch (Exception e) {
                e.printStackTrace();
                this.statusLabel.setText("插件安装失败:" + e.getMessage());
            }
        });
    }
}
