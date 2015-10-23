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

    public SerializableEntry(){}

    public SerializableEntry(Map<String, Object> map){
        super(map);
        Log.d("Entry", "Constructor");
    }

    public SerializableEntry(DropboxAPI.Entry entry){
        this.bytes = entry.bytes;
        this.hash = entry.hash;
        this.isDir = entry.isDir;
        this.modified = entry.modified;
        this.path = entry.path;
        this.root = entry.root;
        this.size = entry.size;
        this.rev = entry.rev;
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

    @Override
    public String toString(){
        StringBuilder result = new StringBuilder();

        result.append(this.getClass().getSimpleName() + " - ");
        result.append(path);
        result.append(" Rev: " + rev);
        return result.toString();
    }
}
