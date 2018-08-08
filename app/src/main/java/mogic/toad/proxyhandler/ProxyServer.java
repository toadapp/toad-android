/**
 * Copyright (c) 2013, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mogic.toad.proxyhandler;

import android.util.Log;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @hide
 */
public class ProxyServer extends Thread {

    private static final String TAG = "ProxyServer";

    private ExecutorService threadExecutor;

    private boolean mIsRunning = false;

    private ServerSocket mServerSocket;
    private int mPort;
    private IProxyListener mCallback;

    private class ProxyConnection implements Runnable {
        private Socket connection;

        private ProxyConnection(Socket connection) {
            this.connection = connection;
        }

        @Override
        public void run() {
            ConnectionHandler.handleConnection(connection);
        }
    }

    public ProxyServer() {
        threadExecutor = Executors.newCachedThreadPool();
        mPort = -1;
        mCallback = null;
    }

    @Override
    public void run() {
        try {
            mServerSocket = new ServerSocket(0);

            reportPort(mServerSocket.getLocalPort());

            while (mIsRunning) {
                try {
                    Socket socket = mServerSocket.accept();
                    // Only receive local connections.
                    if (socket.getInetAddress().isLoopbackAddress()) {
                        ProxyConnection parser = new ProxyConnection(socket);

                        threadExecutor.execute(parser);
                    } else {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "Failed to start proxy server", e);
        } catch (IOException e1) {
            Log.e(TAG, "Failed to start proxy server", e1);
        }

        mIsRunning = false;
    }

    protected synchronized void reportPort(int port) {
        mPort = port;
        if (mCallback != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onReportProxyPort(mPort);
                }
            });
        }
    }

    public synchronized void setCallback(IProxyListener callback) {
        mCallback = callback;
    }

    public synchronized void startServer() {
        mIsRunning = true;
        start();
    }

    public synchronized void stopServer() {
        mIsRunning = false;
        if (mServerSocket != null) {
            try {
                mServerSocket.close();
                mServerSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isBound() {
        return (mPort != -1);
    }

    public int getPort() {
        return mPort;
    }
}
