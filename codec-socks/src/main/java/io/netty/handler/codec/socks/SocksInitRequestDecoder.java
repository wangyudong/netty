/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.socks;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.ArrayList;
import java.util.List;

/**
 * Decodes {@link ByteBuf}s into {@link SocksInitRequest}.
 * Before returning SocksRequest decoder removes itself from pipeline.
 */
public class SocksInitRequestDecoder extends ReplayingDecoder<SocksInitRequestDecoder.State> {
    private static final String name = "SOCKS_INIT_REQUEST_DECODER";

    public static String getName() {
        return name;
    }

    private final List<SocksAuthScheme> authSchemes = new ArrayList<SocksAuthScheme>();
    private SocksProtocolVersion version;
    private byte authSchemeNum;
    private SocksRequest msg = SocksCommonUtils.UNKNOWN_SOCKS_REQUEST;

    public SocksInitRequestDecoder() {
        super(State.CHECK_PROTOCOL_VERSION);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception {
        switch (state()) {
            case CHECK_PROTOCOL_VERSION: {
                version = SocksProtocolVersion.fromByte(byteBuf.readByte());
                if (version != SocksProtocolVersion.SOCKS5) {
                    break;
                }
                checkpoint(State.READ_AUTH_SCHEMES);
            }
            case READ_AUTH_SCHEMES: {
                authSchemes.clear();
                authSchemeNum = byteBuf.readByte();
                for (int i = 0; i < authSchemeNum; i++) {
                    authSchemes.add(SocksAuthScheme.fromByte(byteBuf.readByte()));
                }
                msg = new SocksInitRequest(authSchemes);
                break;
            }
        }
        ctx.pipeline().remove(this);
        out.add(msg);
    }

    enum State {
        CHECK_PROTOCOL_VERSION,
        READ_AUTH_SCHEMES
    }
}
