package io.taucoin.android.di.modules;

import android.content.Context;

import io.taucoin.android.datasource.LevelDbDataSource;
import io.taucoin.android.db.InMemoryBlockStore;
import io.taucoin.android.db.OrmLiteBlockStoreDatabase;
import io.taucoin.config.SystemProperties;
import io.taucoin.core.Account;
import io.taucoin.core.Blockchain;
import io.taucoin.core.BlockchainImpl;
import io.taucoin.core.PendingState;
import io.taucoin.core.PendingStateImpl;
import io.taucoin.core.Repository;
import io.taucoin.core.Wallet;
import io.taucoin.datasource.mapdb.MapDBFactory;
import io.taucoin.datasource.mapdb.MapDBFactoryImpl;
import io.taucoin.db.BlockStore;
import io.taucoin.db.RepositoryImpl;
import io.taucoin.facade.Taucoin;
import io.taucoin.forge.BlockForger;
import io.taucoin.listener.CompositeEthereumListener;
import io.taucoin.listener.EthereumListener;
import io.taucoin.manager.AdminInfo;
import io.taucoin.android.manager.BlockLoader;
import io.taucoin.manager.WorldManager;
import io.taucoin.net.MessageQueue;
import io.taucoin.net.client.PeerClient;
import io.taucoin.net.tau.handler.Tau60;
import io.taucoin.net.tau.handler.Tau61;
import io.taucoin.net.tau.handler.Tau62;
import io.taucoin.net.tau.handler.TauHandlerFactory;
import io.taucoin.net.tau.handler.TauHandlerFactoryImpl;
import io.taucoin.net.p2p.P2pHandler;
import io.taucoin.net.peerdiscovery.DiscoveryChannel;
import io.taucoin.net.peerdiscovery.PeerDiscovery;
import io.taucoin.net.peerdiscovery.WorkerThread;
import io.taucoin.net.rlpx.MessageCodec;
import io.taucoin.net.rlpx.discover.NodeManager;
import io.taucoin.net.rlpx.discover.PeerConnectionTester;
import io.taucoin.net.rlpx.discover.UDPListener;
import io.taucoin.net.server.ChannelManager;
import io.taucoin.net.server.TauChannelInitializer;
import io.taucoin.net.server.PeerServer;
import io.taucoin.sync.PeersPool;
import io.taucoin.sync.SyncManager;
import io.taucoin.sync.SyncQueue;
import io.taucoin.validator.BlockHeaderRule;
import io.taucoin.validator.BlockHeaderValidator;
import io.taucoin.validator.DependentBlockHeaderRule;
import io.taucoin.validator.DifficultyRule;
import io.taucoin.validator.ParentBlockHeaderValidator;
import io.taucoin.validator.ParentNumberRule;
import io.taucoin.validator.ProofOfTransactionRule;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static java.util.Arrays.asList;
import static io.taucoin.config.SystemProperties.CONFIG;

@Module
public class TaucoinModule {

    private Context context;

    boolean storeAllBlocks;
    static WorldManager worldManager = null;
    static Taucoin taucoin = null;

    public TaucoinModule(Context context) {

        this.context = context;
        this.storeAllBlocks = false;
    }

    public TaucoinModule(Context context,boolean storeAllBlocks) {

        this.context = context;
        this.storeAllBlocks = storeAllBlocks;
    }

    @Provides
    @Singleton
    WorldManager provideWorldManager(EthereumListener listener, Blockchain blockchain, Repository repository, Wallet wallet, PeerDiscovery peerDiscovery
            , BlockStore blockStore, ChannelManager channelManager, AdminInfo adminInfo, NodeManager nodeManager, SyncManager syncManager
            , PendingState pendingState) {

        return new WorldManager(listener, blockchain, repository, wallet, peerDiscovery, blockStore, channelManager, adminInfo, nodeManager, syncManager, pendingState);
    }

    @Provides
    @Singleton
    Taucoin provideTaucoin(WorldManager worldManager, AdminInfo adminInfo,
                             ChannelManager channelManager, io.taucoin.manager.BlockLoader blockLoader, PendingState pendingState,
                             Provider<PeerClient> peerClientProvider, Provider<UDPListener> discoveryServerProvider,
                             PeerServer peerServer, BlockForger blockForger) {

        return new io.taucoin.android.Taucoin(worldManager, adminInfo, channelManager, blockLoader, pendingState, peerClientProvider,
                discoveryServerProvider, peerServer, blockForger);
    }

    @Provides
    @Singleton
    io.taucoin.core.Blockchain provideBlockchain(BlockStore blockStore, io.taucoin.core.Repository repository,
                                                   Wallet wallet, AdminInfo adminInfo,
                                                   ParentBlockHeaderValidator parentHeaderValidator, PendingState pendingState, EthereumListener listener) {
        return new BlockchainImpl(blockStore, repository, wallet, adminInfo, parentHeaderValidator, pendingState, listener);
    }

    @Provides
    @Singleton
    Wallet provideWallet(Repository repository, Provider<Account> accountProvider) {
        return new Wallet(repository, accountProvider);
    }

    @Provides
    @Singleton
    BlockStore provideBlockStore() {
        OrmLiteBlockStoreDatabase database = OrmLiteBlockStoreDatabase.getHelper(context);
        return new InMemoryBlockStore(database, storeAllBlocks);
    }

    @Provides
    @Singleton
    Repository provideRepository() {
        LevelDbDataSource stateDS = new LevelDbDataSource();
        return new RepositoryImpl(stateDS);
    }

    @Provides
    @Singleton
    SyncManager provideSyncManagery(Blockchain blockchain, SyncQueue queue, NodeManager nodeManager, EthereumListener taucoinListener
            , PeersPool pool) {
        return new SyncManager(blockchain, queue, nodeManager, taucoinListener, pool);
    }

    @Provides
    @Singleton
    PeersPool providePeersPool() {
        return new PeersPool();
    }

    @Provides
    @Singleton
    SyncQueue provideSyncQueue(Blockchain blockchain, BlockHeaderValidator headerValidator) {
        return new SyncQueue(blockchain, headerValidator);
    }

    @Provides
    BlockHeaderValidator provideBlockHeaderValidator() {
        List<BlockHeaderRule> rules = new ArrayList<BlockHeaderRule>();
        rules.add(new ProofOfTransactionRule());

        return new BlockHeaderValidator(rules);
    }

    @Provides
    @Singleton
    AdminInfo provideAdminInfo() {
        return new AdminInfo();
    }

    @Provides
    @Singleton
    EthereumListener provideEthereumListener() {
        return new CompositeEthereumListener();
    }

    @Provides
    @Singleton
    PeerDiscovery providePeerDiscovery() {
        return new PeerDiscovery();
    }

    @Provides
    @Singleton
    ChannelManager provideChannelManager(EthereumListener listener, SyncManager syncManager, PendingState pendingState) {
        return new ChannelManager(listener, syncManager, pendingState);
    }

    @Provides
    @Singleton
    NodeManager provideNodeManager(PeerConnectionTester peerConnectionManager, MapDBFactory mapDBFactory, EthereumListener listener) {
        return new NodeManager(peerConnectionManager, mapDBFactory, listener);
    }

    @Provides
    @Singleton
    PeerConnectionTester providePeerConnectionTester() {
        return new PeerConnectionTester();
    }

    @Provides
    @Singleton
    MapDBFactory provideMapDBFactory() {
        return new MapDBFactoryImpl();
    }


    @Provides
    @Singleton
    BlockLoader provideBlockLoader(Blockchain blockchain) {
        return new BlockLoader(blockchain);
    }

    @Provides
    @Singleton
    PendingState providePendingState(EthereumListener listener, Repository repository) {
        return new PendingStateImpl(listener, repository);
    }

    @Provides
    Account provideAccount(Repository repository) {
        return new Account(repository);
    }

    @Provides
    P2pHandler provideP2pHandler(PeerDiscovery peerDiscovery, EthereumListener listener) {
        return new P2pHandler(peerDiscovery, listener);
    }

    @Provides
    MessageCodec provideMessageCodec(EthereumListener listener) {
        return new MessageCodec(listener);
    }

    @Provides
    PeerClient providePeerClient(EthereumListener listener, ChannelManager channelManager,
                                 Provider<TauChannelInitializer> taucoinChannelInitializerProvider) {
        return new PeerClient(listener, channelManager, taucoinChannelInitializerProvider);
    }

    @Provides
    @Singleton
    PeerServer providePeerServer(ChannelManager channelManager, TauChannelInitializer taucoinChannelInitializer,
            EthereumListener listener) {
        return new PeerServer(channelManager, taucoinChannelInitializer, listener);
    }

    @Provides
    MessageQueue provideMessageQueue(EthereumListener listener) {
        return new MessageQueue(listener);
    }

    @Provides
    WorkerThread provideWorkerThread(Provider<DiscoveryChannel> discoveryChannelProvider) {
        return new WorkerThread(discoveryChannelProvider);
    }

    @Provides
    String provideRemoteId() {
        return SystemProperties.CONFIG.peerActive().get(0).getHexId();
    }

    @Provides
    @Singleton
    Context provideContext() {
        return context;
    }

    @Provides
    Tau60 provideTau60() { return new Tau60(); }

    @Provides
    Tau61 provideTau61() { return new Tau61(); }

    @Provides
    Tau62 provideTau62() { return new Tau62(); }

    @Provides
    @Singleton
    TauHandlerFactory provideTauHandlerFactory(Provider<Tau60> eth60Provider, Provider<Tau61> eth61Provider, Provider<Tau62> eth62Provider) {
        return new TauHandlerFactoryImpl(eth60Provider, eth61Provider, eth62Provider);
    }

    @Provides
    @Singleton
    ParentBlockHeaderValidator provideParentBlockHeaderValidator() {

        List<DependentBlockHeaderRule> rules = new ArrayList<>(asList(
                new ParentNumberRule(),
                new DifficultyRule()
        ));

        return new ParentBlockHeaderValidator(rules);
    }

    @Provides
    @Singleton
    BlockForger provideBlockForger() {
        return new BlockForger();
    }
}