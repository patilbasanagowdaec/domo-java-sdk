package com.domo.sdk;

import com.domo.sdk.datasets.DataSetClient;
import com.domo.sdk.datasets.model.Column;
import com.domo.sdk.datasets.model.CreateDataSetRequest;
import com.domo.sdk.datasets.model.DataSet;
import com.domo.sdk.datasets.model.DataSetListResult;
import com.domo.sdk.datasets.model.Schema;
import com.domo.sdk.groups.GroupClient;
import com.domo.sdk.groups.model.Group;
import com.domo.sdk.groups.model.UpdateGroupRequest;
import com.domo.sdk.request.Config;
import com.domo.sdk.request.Scope;
import com.domo.sdk.streams.StreamDataSetClient;
import com.domo.sdk.streams.model.StreamDataSet;
import com.domo.sdk.streams.model.StreamDataSetRequest;
import com.domo.sdk.streams.model.StreamUploadMethod;
import com.domo.sdk.users.UserClient;
import com.domo.sdk.users.model.CreateUserRequest;
import com.domo.sdk.users.model.User;
import okhttp3.logging.HttpLoggingInterceptor;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static com.domo.sdk.datasets.model.ColumnType.STRING;
import static com.domo.sdk.request.Scope.DATA;
import static com.domo.sdk.request.Scope.USER;

public class ClientTest {

    private Client client;

    @Before
    public void setup() {
        Config rigConf = Config.with()
                .clientId("d3090add-ebe5-4d6f-bfb0-2417a54d9ea2")
                .clientSecret("50c40c6f25b26da321e1dae8aeb1e057f72673a34d0d7fba943f3d726df3f913")
                .apiHost("localhost:19999")
                .useHttps(false)
                .scope(USER, DATA)
                .httpLoggingLevel(HttpLoggingInterceptor.Level.BODY)
                .build();

        Config prodConf = Config.with()
                .clientId("64e0e76e-4916-4cd3-97cb-c55573c236ba")
                .clientSecret("41c7e09fea52a8cc70b51bb3450841228631ad2dd55952baa5d068110e0e8595")
                .httpLoggingLevel(HttpLoggingInterceptor.Level.BODY)
                .scope(USER, DATA)
                .build();

        Config dev3Conf = Config.with()
                .clientId("d2386c10-69a6-41c4-983d-f094d019e532")
                .clientSecret("cf1b7671b456c06f0c68d54e16b72bf27033103205eb0c8d925905c5d8fde24e")
                .apiHost("api.dev.domo.com")
                .scope(USER, DATA)
                .httpLoggingLevel(HttpLoggingInterceptor.Level.BODY)
                .build();


        //Education.domo.com
        client = Client.create(dev3Conf);
//        client = Client.create("dd2ccdab-4120-4885-be37-d24088de03c2", "72befda1108f59c94e0c91bdcc53f4018e45f8b47e8ef1fa9aa769999a46483b");
    }

    @Test
    public void groupClient_smokeTest() {
        GroupClient gClient = client.groupClient();

        Group testGroup = new Group();
        testGroup.setName("Test Group"+System.currentTimeMillis());

        Group g2 = gClient.create(testGroup);
        System.out.println("create:"+g2);

        Group g3 = gClient.get(g2.getId());
        System.out.println("get:"+g3);

        List<Group> list = gClient.list(10,0);
        System.out.println("list:"+list);

        UpdateGroupRequest ugroup = new UpdateGroupRequest();
        ugroup.setName("DeleteMe"+System.currentTimeMillis());
        ugroup.setActive(true);
        Group g4 = gClient.update(g3.getId(), ugroup);
        System.out.println("Update:"+g4);

        User user = client.userClient().create(false,
                new CreateUserRequest("deleteme.user"+System.currentTimeMillis()+"@domo.com","Participant", "Delete Me - Smoke User"));

        System.out.println("Add user to group");
        gClient.addUserToGroup(g4.getId(), user.getId());

        System.out.println("List users in group");
        gClient.listUsersInGroup(g4.getId());

        System.out.println("Remove User");
        gClient.removeUserFromGroup(g4.getId(), user.getId());

        System.out.println("List Users after removing them");
        gClient.listUsersInGroup(g4.getId());

        gClient.delete(g3.getId());
        client.userClient().delete(user.getId());
    }

    @Test
    public void userClient_smokeTest() throws IOException {
        UserClient userClient = client.userClient();

        List<User> list = userClient.list(30, 0);
        System.out.println(list);


//        User user = userClient.create(false, new CreateUserRequest("api.smoke.test@domo.com", "Admin", "API Smoke Test"));
//        System.out.println("Created user:"+user);

        User user2 = userClient.get(669096686L);
        System.out.println("Get user:"+user2);

        user2.setName("Clint Checketts");
        User user3 = userClient.update(user2.getId(), user2);
        System.out.println("Updated user:"+user3);

//        userClient.delete(user.getId());
    }

    @Test
    public void dataSetClient_smokeTest() throws IOException {
        DataSetClient dsClient = client.dataSetClient();

        //Create DS
        CreateDataSetRequest createRequest = new CreateDataSetRequest();
        createRequest.setName("Smoke Test DataSet");
        createRequest.setDescription("FTW!");
        createRequest.setSchema(new Schema(Lists.newArrayList(new Column(STRING, "name"))));

        DataSet ds = dsClient.create(createRequest);
        System.out.println("Created:"+ds);

        //Get DS
        DataSet ds2 = dsClient.get(ds.getId());
        System.out.println("Get:"+ds2);

        //Update DS
        ds.setName("Smoke Test DataSet Update");
        ds.setDescription("FTW! FTW! Updated");
        ds.getSchema().setColumns(Lists.newArrayList(new Column(STRING, "1st Letters"),
                new Column(STRING, "2nd Letters"),
                new Column(STRING, "3rd Letters")));
        dsClient.update(ds);

        //Import DS
        String input = "\"a\",\"b\",\"c\"\n\"d\",\"e\",\"f\"\n\"g\",\"h\",\"i\"\n\"j\",\"k\",\"l\"\n\"m\",\"n\",\"o\"\n\"p\",\"q\",\"r\"";
        dsClient.importData(ds.getId(),input);

        //Export DS
        InputStream stream = dsClient.exportData(ds.getId(),true);
        String data = convertStreamToString(stream);
        stream.close();
        System.out.println(data);

        //Export to file
        File f = File.createTempFile("sample-export", ".csv");
        dsClient.exportData(ds.getId(),true, f);
        System.out.println("Wrote out file:"+f.getAbsolutePath());


        //Policies

        //List DS
        List<DataSetListResult> list = dsClient.list(5,0);
        System.out.println(list);

        //Delete DS
        dsClient.delete(ds.getId());
    }

    @Test
    public void streamDataSetClient_smokeTest() throws IOException {
        StreamDataSetClient sdsClient = client.streamDataSetClient();

        //Create Stream
        DataSet ds = new DataSet();
        ds.setName("Leonhard Euler Party");
        ds.setDescription("Mathematician Guest List");
        Schema schema = new Schema();
        List<Column> columns = new ArrayList<>();
        columns.add(new Column(STRING, "Friend"));
        columns.add(new Column(STRING, "Attending"));
        schema.setColumns(columns);

        StreamDataSetRequest sdsRequest = new StreamDataSetRequest();
        sdsRequest.setDataset(ds);
        sdsRequest.setUpdateMethod(StreamUploadMethod.APPEND);
        StreamDataSet fullSds = sdsClient.createStreamDataset(sdsRequest);
        System.out.println("Created:" + fullSds);

        //Get Stream



        //sdsClient.getStreamDataset();

        //List Streams

        //Search Streams

        //Update Stream

        //Create Execution

        //Get Execution

        //List Executions

        //Upload Parts

        //Commit Execution

        //Delete Stream
    }

    private static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}