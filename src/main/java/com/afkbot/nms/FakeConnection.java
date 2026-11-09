package com.afkbot.nms;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;

/**
 * 假网络连接：真实玩家的连接会收发数据包，
 * 假人没有客户端，所有出站数据包直接丢弃。
 */
public final class FakeConnection extends Connection {

    public FakeConnection() {
        super(PacketFlow.SERVERBOUND);
        // channel / address 在 26.3 中是私有字段，通过反射注入假实现
        try {
            Field fChannel = Connection.class.getDeclaredField("channel");
            fChannel.setAccessible(true);
            fChannel.set(this, new EmbeddedChannel());

            Field fAddress = Connection.class.getDeclaredField("address");
            fAddress.setAccessible(true);
            fAddress.set(this, new InetSocketAddress("127.0.0.1", 0));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("初始化假连接失败", e);
        }
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void send(Packet<?> packet) {
        // 丢弃：假人没有真实客户端
    }

    @Override
    public void send(Packet<?> packet, ChannelFutureListener listener) {
        // 丢弃
    }

    @Override
    public void send(Packet<?> packet, ChannelFutureListener listener, boolean flush) {
        // 丢弃
    }
}
