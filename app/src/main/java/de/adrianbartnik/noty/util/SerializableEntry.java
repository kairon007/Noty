package de.adrianbartnik.noty.util;

import android.util.Log;

import com.dropbox.client2.DropboxAPI;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

public class SerializableEntry extends DropboxAPI.Entry implements Externalizable{

    static final long serialVersionUID = 4209360273818925922L;

    public SerializableEntry(Map<String, Object> map){
        super(map);
        Log.d("Entry", "Constructor");
    }

    public SerializableEntry(){

    }

    @Override
    public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
        Log.d("Entry", "Entry: Read");
        this.bytes = input.readLong();
        this.hash = (String) input.readObject();
        this.isDir = input.readBoolean();
        this.modified = (String) input.readObject();
        this.path = (String) input.readObject();
        this.root = (String) input.readObject();
        this.size = (String) input.readObject();
        this.rev = (String) input.readObject();
    }

    @Override
    public void writeExternal(ObjectOutput output) throws IOException {
        Log.d("Entry", "Entry: Write");
        output.writeLong(bytes);
        output.writeObject(hash);
        output.writeBoolean(isDir);
        output.writeObject(modified);
        output.writeObject(path);
        output.writeObject(root);
        output.writeObject(size);
        output.writeObject(rev);
    }
}
