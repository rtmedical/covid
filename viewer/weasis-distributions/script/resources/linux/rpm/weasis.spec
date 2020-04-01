Summary: APPLICATION_SUMMARY
Name: APPLICATION_PACKAGE
Version: APPLICATION_VERSION
Release: APPLICATION_RELEASE
License: APPLICATION_LICENSE_TYPE
Vendor: APPLICATION_VENDOR
Prefix: %{dirname:APPLICATION_DIRECTORY}
Provides: APPLICATION_PACKAGE
%if "xAPPLICATION_GROUP" != x
Group: APPLICATION_GROUP
%endif

Autoprov: 0
Autoreq: 0
%if "xPACKAGE_CUSTOM_DEPENDENCIES" != x
Requires: PACKAGE_CUSTOM_DEPENDENCIES
%endif

#avoid ARCH subfolder
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.%%{ARCH}.rpm

#comment line below to enable effective jar compression
#it could easily get your package size from 40 to 15Mb but
#build time will substantially increase and it may require unpack200/system java to install
%define __jar_repack %{nil}

%description
APPLICATION_DESCRIPTION

%prep

%build

%install
rm -rf %{buildroot}
install -d -m 755 %{buildroot}APPLICATION_DIRECTORY
cp -r %{_sourcedir}APPLICATION_DIRECTORY/* %{buildroot}APPLICATION_DIRECTORY
%if "xAPPLICATION_LICENSE_FILE" != x
  %define license_install_file %{_defaultlicensedir}/%{name}-%{version}/%{basename:APPLICATION_LICENSE_FILE}
  install -d -m 755 %{buildroot}%{dirname:%{license_install_file}}
  install -m 644 APPLICATION_LICENSE_FILE %{buildroot}%{license_install_file}
%endif

%files
%if "xAPPLICATION_LICENSE_FILE" != x
  %license %{license_install_file}
  %{dirname:%{license_install_file}}
%endif
# If installation directory for the application is /a/b/c, we want only root
# component of the path (/a) in the spec file to make sure all subdirectories
# are owned by the package.
%(echo APPLICATION_DIRECTORY | sed -e "s|\(^/[^/]\{1,\}\).*$|\1|")

%post
DESKTOP_COMMANDS_INSTALL
xdg-desktop-menu install /opt/weasis/lib/weasis-Dicomizer.desktop	
mkdir -p /etc/opt/chrome/policies/managed/
    echo '{
    "URLWhitelist": ["weasis://*"]
}' > /etc/opt/chrome/policies/managed/weasis.json 
mkdir -p /etc/chromium/policies/managed/
cp /etc/opt/chrome/policies/managed/weasis.json /etc/chromium/policies/managed/weasis.json

%preun
UTILITY_SCRIPTS
DESKTOP_COMMANDS_UNINSTALL
xdg-desktop-menu uninstall /opt/weasis/lib/weasis-Dicomizer.desktop
rm -f /etc/opt/chrome/policies/managed/weasis.json 
rm -f /etc/chromium/policies/managed/weasis.json

%clean
