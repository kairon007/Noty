package de.adrianbartnik.noty.util;

import com.dropbox.client2.DropboxAPI;

import java.io.Serializable;

public class SerializableEntry implements Serializable, Comparable{

    static final long serialVersionUID = 4209360273818925922L;

    public long bytes;
    public String hash;
    public boolean isDir;
    public String modified;
    public String path;
    public String root;
    public String size;
    public String rev;

    public SerializableEntry(DropboxAPI.Entry entry) {
        this.bytes = entry.bytes;
        this.hash = entry.hash;
        this.isDir = entry.isDir;
        this.modified = entry.modified;
        this.path = makePathCaseInsensitive(entry.path);
        this.root = entry.root;
        this.size = entry.size;
        this.rev = entry.rev;
    }

    private String makePathCaseInsensitive(String path){

        String result = "";
        int index = path.lastIndexOf("/");
        while (index > 0){
            result = path.substring(index + 1, index + 2).toUpperCase() + path.substring(index + 2).toLowerCase() + "/" + result;
            path = path.substring(0, index);
            index = path.lastIndexOf("/");
            // System.out.println("Path: " + path + " Result: " + result + " Index: " + index);
        }
        if(result.length() == 0)
            return "/" + path.substring(1, 2).toUpperCase() + path.substring(2).toLowerCase();
        else
            return "/" + path.substring(1, 2).toUpperCase() + path.substring(2).toLowerCase() + "/" + result.substring(0, result.length() - 1);
    }

    public String fileName() {
        int ind = this.path.lastIndexOf(47);
        return this.path.substring(ind + 1, this.path.length());
    }

    public String parentPath() {
        if (this.path.equals("/")) {
            return "";
        } else {
            int ind = this.path.lastIndexOf(47);
            return this.path.substring(0, ind + 1);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SerializableEntry that = (SerializableEntry) o;

        return path.equals(that.path);

    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public String toString() {
        return path + ( isDir ? " Rev: " + rev : " Hash: " + hash);
    }

    @Override
    public int compareTo(Object another) {
        if(another instanceof SerializableEntry){
            return fileName().compareTo(((SerializableEntry) another).fileName());
        } else
            return 0;
    }
}
