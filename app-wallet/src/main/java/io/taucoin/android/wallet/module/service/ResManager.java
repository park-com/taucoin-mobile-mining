/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.android.wallet.module.service;

import android.content.Context;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageStats;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Message;
import android.support.annotation.NonNull;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.BaseHandler;
import io.taucoin.android.wallet.util.SysUtil;

 class ResManager implements BaseHandler.HandleCallBack{

     private boolean isRunning = true;
     private IBinder mBinder = null;
     private IInterface mInterface = null;

     private BaseHandler mHandler;
     private ResCallBack mResCallBack;

     ResManager(){
         mHandler = new BaseHandler(this);
     }
     @Override
     public void handleMessage(Message msg) {
         switch (msg.what) {
             case 2:
                 Bundle bundle = msg.getData();
                 SysUtil.MemoryInfo info = bundle.getParcelable("data");
                 if(info != null){
                     String memoryInfo = SysUtil.formatFileSize(info.totalMemory);
                     String cpuInfo = String.valueOf(info.cpuUsageRate);
                     int pointIndex = cpuInfo.indexOf(".");
                     int length = cpuInfo.length();
                     if(pointIndex > 0 && length - pointIndex > 3){
                         cpuInfo = cpuInfo.substring(0, pointIndex + 3);
                     }
                     cpuInfo += "%";
                     if(mResCallBack != null){
                         mResCallBack.updateCpuAndMemory(cpuInfo, memoryInfo);
                     }
                 }
                 break;
             case 3:
                 PackageStats newPs = msg.getData().getParcelable("data");
                 if (newPs != null) {
                     long dataSize = newPs.dataSize + newPs.cacheSize;
                     String dataInfo = SysUtil.formatFileSize(dataSize);
                     if(mResCallBack != null){
                         mResCallBack.updateDataSize(dataInfo);
                     }
                 }
                 break;
             default:
                 break;
         }
     }

    void startResThread(ResCallBack resCallBack) {
        mResCallBack = resCallBack;
        SysUtil sysUtil = new SysUtil();
        Thread mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(isRunning){
                        Context context = MyApplication.getInstance();
                        sysUtil.getPkgInfo(context.getPackageName(), packageStatsObserver);

                        SysUtil.MemoryInfo info =  sysUtil.loadAppProcess();
                        Bundle bundle = new Bundle();
                        bundle.putParcelable("data", info);
                        Message message = mHandler.obtainMessage(2);
                        message.setData(bundle);
                        mHandler.sendMessage(message);
                        Thread.sleep(1000);
                        this.run();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        mThread.start();
    }

     private IPackageStatsObserver.Stub packageStatsObserver = new IPackageStatsObserver.Stub() {
         public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) {
             Message msg = mHandler.obtainMessage(3);
             Bundle data = new Bundle();
             data.putParcelable("data", pStats);
             msg.setData(data);
             mHandler.sendMessage(msg);
         }

         @Override
         public IBinder asBinder() {
             mBinder = super.asBinder();
             return mBinder;
         }

         @Override
         public IInterface queryLocalInterface(@NonNull String descriptor) {
             mInterface = super.queryLocalInterface(descriptor);
             return mInterface;
         }
     };

     void stopResThread() {
         isRunning = false;
         if(mInterface != null){
             mBinder = mInterface.asBinder();
         }
         mBinder = null;
         mInterface = null;
     }

     abstract static class ResCallBack{
         abstract void updateCpuAndMemory(String cpuInfo, String memoryInfo);
         abstract void updateDataSize(String dataInfo);
     }
 }