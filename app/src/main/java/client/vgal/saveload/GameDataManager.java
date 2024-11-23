package client.vgal.saveload;

import client.vgal.utils.FileUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class GameDataManager implements Serializable {

    private File savePath;

    public GameDataManager(File savePath){
        this.savePath = savePath;
        SerializeConfig config = new SerializeConfig();
        config.put(int.class, new IntToStringSerializer());
        config.put(Integer.class, new IntToStringSerializer());
    }

    public static class IntToStringSerializer implements ObjectSerializer {
        @Override
        public void write(com.alibaba.fastjson.serializer.JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
            if (object instanceof Integer) {
                // 将 int 转换为 String
                serializer.write(String.valueOf(object));
            } else {
                serializer.write(object);
            }
        }
    }


    public boolean SaveData(int slot,GameData gameData){
        try{
            File file = new File(savePath,"gameData.json");
            gameData.setSaveTime(LocalDateTime.now());

            Map<Integer,GameData> map = getSaveData();
            map.put(slot,gameData);

            if(!file.exists()){
                savePath.mkdirs();
                file.createNewFile();
            }

            JSON.writeJSONString(new FileOutputStream(file),map, SerializerFeature.QuoteFieldNames);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }

    public GameData LoadData(int slot){
        return getSaveData().get(slot);
    }

    public Map<Integer,GameData> getSaveData(){
        File file = new File(savePath,"gameData.json");
        if (!file.exists()){
            return new HashMap<>();
        }

        return JSON.parseObject(FileUtil.readFileUTF8(file.getAbsolutePath())
                , new TypeReference<Map<Integer,GameData>>(){});
    }

    public Map<String,GameData> getSaveData2(){
        Map<Integer, GameData> saveData = getSaveData();
        Map<String,GameData> saveData2 = new HashMap<>();
        for (Map.Entry<Integer, GameData> entry : saveData.entrySet()){
            saveData2.put(entry.getKey()+"",entry.getValue());
        }
        return saveData2;
    }

}
