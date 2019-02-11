package io.taucoin.android_app;

import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import io.taucoin.android.interop.AdminInfo;
import io.taucoin.android.service.ConnectorHandler;
import io.taucoin.android.service.TaucoinClientMessage;
import io.taucoin.android.service.events.EventFlag;
import org.ethereum.config.SystemProperties;
import org.ethereum.net.rlpx.Node;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.UUID;

import static org.ethereum.config.SystemProperties.CONFIG;

public class TestsFragment extends Fragment implements ConnectorHandler {

    TextView connectionStatus;
    TextView blockchainStatus;
    TextView startupTime;
    TextView isConsensus;
    TextView blockExecTime;

    Button discoverytButton;
    Button connectButton;
    Button getEthereumStatus;
    Button getBlockchainStatus;

    String identifier = UUID.randomUUID().toString();

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_tests, container, false);

        connectionStatus = (TextView)view.findViewById(R.id.connectionStatus);
        blockchainStatus = (TextView)view.findViewById(R.id.blockchainStatus);
        startupTime = (TextView)view.findViewById(R.id.startupTime);
        isConsensus = (TextView)view.findViewById(R.id.isConsensus);
        blockExecTime = (TextView)view.findViewById(R.id.blockExecTime);
        discoverytButton = (Button)view.findViewById(R.id.discoveryButton);
        connectButton = (Button)view.findViewById(R.id.connectButton);
        getEthereumStatus = (Button)view.findViewById(R.id.getEthereumStatus);
        getBlockchainStatus = (Button)view.findViewById(R.id.getBlockchainStatus);

        discoverytButton.setOnClickListener(onClickListener);
        connectButton.setOnClickListener(onClickListener);
        getEthereumStatus.setOnClickListener(onClickListener);
        getBlockchainStatus.setOnClickListener(onClickListener);

        return view;
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {

            switch(v.getId()){
                case R.id.discoveryButton:
                    TaucoinApplication.ethereumConnector.startPeerDiscovery();
                    break;
                case R.id.connectButton:
                    Node node = CONFIG.peerActive().get(0);
                    TaucoinApplication.ethereumConnector.connect(node.getHost(), node.getPort(), node.getHexId());
                    break;
                case R.id.getEthereumStatus:
                    TaucoinApplication.ethereumConnector.getConnectionStatus(identifier);
                    TaucoinApplication.ethereumConnector.getAdminInfo(identifier);
                    break;
                case R.id.getBlockchainStatus:
                    TaucoinApplication.ethereumConnector.getBlockchainStatus(identifier);
                    break;
            }
        }
    };

    protected void updateTextView(final TextView view, final String text) {

        view.post(new Runnable() {
            @Override
            public void run() {
                view.setText(text);
            }
        });
    }

    @Override
    public boolean handleMessage(final Message message) {

        boolean isClaimed = true;
        switch(message.what) {
            case TaucoinClientMessage.MSG_CONNECTION_STATUS:
                updateTextView(connectionStatus, message.getData().getString("status"));
                break;
            case TaucoinClientMessage.MSG_BLOCKCHAIN_STATUS:
                updateTextView(blockchainStatus, message.getData().getString("status"));
                break;
            case TaucoinClientMessage.MSG_ADMIN_INFO:
                Bundle data = message.getData();
                data.setClassLoader(AdminInfo.class.getClassLoader());
                AdminInfo adminInfo = data.getParcelable("adminInfo");
                updateTextView(startupTime, new SimpleDateFormat("yyyy MM dd HH:mm:ss").format(new Date(adminInfo.getStartupTimeStamp())));
                updateTextView(isConsensus, adminInfo.isConsensus() ? "true" : "false");
                updateTextView(blockExecTime, adminInfo.getExecAvg().toString());
                break;
            case TaucoinClientMessage.MSG_ONLINE_PEER:
                break;
            case TaucoinClientMessage.MSG_PEERS:
                break;
            case TaucoinClientMessage.MSG_PENDING_TRANSACTIONS:
                break;
            case TaucoinClientMessage.MSG_SUBMIT_TRANSACTION_RESULT:
                break;
            default:
                isClaimed = false;
        }
        return isClaimed;
    }

    @Override
    public String getID() {

        return identifier;
    }

    @Override
    public void onConnectorConnected() {

        //app.ethereum.addListener(identifier, EnumSet.allOf(EventFlag.class));
        //Node node = SystemProperties.CONFIG.peerActive().get(0);
        //app.ethereum.connect(node.getHost(), node.getPort(), node.getHexId());
    }

    @Override
    public void onConnectorDisconnected() {

    }

}