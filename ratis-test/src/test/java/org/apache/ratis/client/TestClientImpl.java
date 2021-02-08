/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ratis.client;


import org.apache.ratis.BaseTest;
import org.apache.ratis.RaftConfigKeys;
import org.apache.ratis.conf.Parameters;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.grpc.GrpcFactory;
import org.apache.ratis.protocol.*;
import org.apache.ratis.retry.RetryPolicies;
import org.apache.ratis.retry.RetryPolicy;
import org.apache.ratis.rpc.SupportedRpcType;
import org.apache.ratis.util.TimeDuration;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Test {@link org.apache.ratis.client.impl.RaftClientImpl}. */
public class TestClientImpl extends BaseTest {
  // Sample group
  private static final List<RaftPeer> PEERS = new ArrayList<>(3);
  static {
    PEERS.add(RaftPeer.newBuilder().setId("n1").setAddress("127.0.0.1:6000").build());
    PEERS.add(RaftPeer.newBuilder().setId("n2").setAddress("127.0.0.1:6001").build());
    PEERS.add(RaftPeer.newBuilder().setId("n3").setAddress("127.0.0.1:6002").build());
  }
  private static final UUID CLUSTER_GROUP_ID = UUID.fromString("02511d47-d67c-49a3-9011-abb3109a44c1");
  private static final RaftGroup RAFT_GROUP = RaftGroup.valueOf(
          RaftGroupId.valueOf(CLUSTER_GROUP_ID), PEERS);

  @Test
  public void testGetNewInstance() throws Exception {
    RaftProperties raftProperties = new RaftProperties();
    RaftConfigKeys.Rpc.setType(raftProperties, SupportedRpcType.GRPC);
    RetryPolicy retryPolicy = RetryPolicies.retryUpToMaximumCountWithFixedSleep(10,
            TimeDuration.valueOf(1000, TimeUnit.MILLISECONDS));

    RaftClient raftClient = buildNewClient(raftProperties, retryPolicy);
    Assert.assertEquals(raftProperties, raftClient.getProperties());
    Assert.assertEquals(retryPolicy, raftClient.getRetryPolicy());

    RaftProperties newRaftProperties = new RaftProperties();
    RaftConfigKeys.Rpc.setType(newRaftProperties, SupportedRpcType.NETTY);
    RetryPolicy newRetryPolicy = RetryPolicies.retryUpToMaximumCountWithFixedSleep(100,
            TimeDuration.valueOf(100, TimeUnit.MILLISECONDS));

    RaftClient newRaftClient = raftClient.getNewInstanceWithOptions(newRaftProperties, newRetryPolicy);
    Assert.assertEquals(newRaftProperties, newRaftClient.getProperties());
    Assert.assertEquals(newRetryPolicy, newRaftClient.getRetryPolicy());

    // Check that APIs have fresh objects
    Assert.assertNotEquals(raftClient.admin(), newRaftClient.admin());
    Assert.assertNotEquals(raftClient.async(), newRaftClient.async());
    Assert.assertNotEquals(raftClient.getDataStreamApi(), newRaftClient.getDataStreamApi());
    Assert.assertNotEquals(raftClient.getMessageStreamApi(), newRaftClient.getMessageStreamApi());

    newRaftClient = raftClient.getNewInstanceWithOptions(newRaftProperties);
    Assert.assertEquals(newRaftProperties, newRaftClient.getProperties());
    Assert.assertEquals(retryPolicy, newRaftClient.getRetryPolicy());

    newRaftClient = raftClient.getNewInstanceWithOptions(newRetryPolicy);
    Assert.assertEquals(raftProperties, newRaftClient.getProperties());
    Assert.assertEquals(newRetryPolicy, newRaftClient.getRetryPolicy());
  }
  private RaftClient buildNewClient(RaftProperties raftProperties, RetryPolicy retryPolicy) {
    RaftClient.Builder builder = RaftClient.newBuilder()
            .setProperties(raftProperties).
            setRetryPolicy(retryPolicy)
            .setRaftGroup(RAFT_GROUP)
            .setClientRpc(
                    new GrpcFactory(new Parameters())
                            .newRaftClientRpc(ClientId.randomId(), raftProperties));
    return builder.build();
  }
}