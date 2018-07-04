package personal.nfl.vpn.processparse;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * @author nfl
 * 手机系统中所有的 app 包名
 */

public class PackageNames implements Parcelable, Serializable {

    public final String[] pkgs;

    protected PackageNames(String[] pkgs) {
        this.pkgs = pkgs;
    }

    public static PackageNames newInstance(String[] pkgs) {
        return new PackageNames(pkgs);
    }

    public String getAt(int i) {
        if (this.pkgs.length > i) {
            return this.pkgs[i];
        }
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.pkgs.length);
        dest.writeStringArray(this.pkgs);
    }


    protected PackageNames(Parcel in) {
        this.pkgs = new String[in.readInt()];
        in.readStringArray(this.pkgs);
    }

    public static final Creator<PackageNames> CREATOR = new Creator<PackageNames>() {
        @Override
        public PackageNames createFromParcel(Parcel in) {
            return new PackageNames(in);
        }

        @Override
        public PackageNames[] newArray(int size) {
            return new PackageNames[size];
        }
    };
}
