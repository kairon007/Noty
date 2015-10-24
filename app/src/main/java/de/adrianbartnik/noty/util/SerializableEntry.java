package de.adrianbartnik.noty.util;

import com.dropbox.client2.DropboxAPI;

import java.io.Serializable;

public class SerializableEntry implements Serializable {

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
        this.path = entry.path;
        this.root = entry.root;
        this.size = entry.size;
        this.rev = entry.rev;
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

        if (isDir != that.isDir) return false;
        if (!path.equals(that.path)) return false;
        return rev.equals(that.rev);
    }

    @Override
    public int hashCode() {
        int result = (isDir ? 1 : 0);
        result = 31 * result + path.hashCode();
        result = 31 * result + rev.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return path + " Rev: " + rev;
    }

}
