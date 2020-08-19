package vip.youwe.sheller.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import org.json.JSONObject;
import vip.youwe.sheller.core.ShellService;
import vip.youwe.sheller.dao.ShellManager;

import java.util.List;

public class ParallelViewController {

    @FXML
    private FlowPane hostFlowPane;
    @FXML
    private MenuItem addHostBtn;
    @FXML
    private MenuItem doScanBtn;
    @FXML
    private RadioButton hostViewRadio;
    @FXML
    private RadioButton serviceViewRadio;
    @FXML
    private GridPane hostDetailGridPane;
    @FXML
    private GridPane hostListGridPane;
    @FXML
    private FlowPane serviceDetailFlowPane;
    @FXML
    private Button returnListBtn;
    private ShellService currentShellService;
    private ShellManager shellManager;
    private JSONObject shellEntity;
    private List<Thread> workList;
    private Label statusLabel;
    private ContextMenu hostContextMenu;
    private ContextMenu serviceContextMenu;
}