# Sentinel-2 Toolbox Cloud Executor

This is a simple demonstrator of the cloud execution capabilities of the ESA Sentinel-2 Toolbox.

# Prerequisites - how to configure the virtual machines
Although the demonstrator can run on Windows machines also, below are presented the steps necessary to configure Linux machines.

## Step 1: Create virtual machines
Create 3 virtual machines with CentOS 7.
## Step 2: Install Java
Download the latest JRE from Oracle.
In a terminal window, go to the location where the rpm was downloaded and execute:

    sudo yum localinstall jre-8u92-linux-x64.rpm
    rm jre-8u92-linux-x64.rpm
    sudo alternatives --config java

Choose the appropriate java version:

    sudo sh -c "echo export JAVA_HOME=/usr/java/jre1.8.0_92 >> /etc/environment"

## Step 3: Install SNAP
Download the latest version from http://step.esa.int.
Change the permission of the downloaded file to be executable.
Execute the file using the -c option (in order to install SNAP from command line).
Follow the on-screen instructions.

## Step 4: Create the master shared folder
On the "master" machine open a terminal and do the following:

    yum -y install samba samba-client
    mkdir /mnt/share
    chmod 777 /mnt/share
    vi /etc/samba/smb.conf

Near line 66: add the following:

    unix charset = UTF-8
    dos charset = CP932

Near line 90: change (Windows' default):

    workgroup = WORKGROUP

Near line 96: uncomment and change IP addresses you want to allow.

    hosts allow = 127. 10.0.0.

In the above, the IPs from 127.*.*.* and 10.0.0.* are allowed.
Near line 126: add ( no auth ):

    security = user
    passdb backend = tdbsam
    map to guest = Bad User

Append the followint to the end of the file:

    [share]
      path = /mnt/share
      writable = yes
      guest ok = yes
      guest only = yes
      create mode = 0777
      directory mode = 0777
      share modes = yes

Save and close the file, and then restart (and enable) the smb and nmb services:

    systemctl start smb nmb
    systemctl enable smb nmb

## Step 5: Configure SELinux on the master node
On the master node, run the following in a terminal window:
    chcon -Rt samba_share_t /mnt/share
Reboot the machine:
    reboot

## Step 6: Mount the master shared folder on the slave machines
On each of the "slave" machines mount the shared folder:

    mkdir /home/share
    chmod 777 /home/share
    sudo mount.cifs \\\\<master>\\share /home/share -o user=admin password=abc123.

To persist the shared folder, edit (sudo) the /etc/fstab file and add the following line:

    //<master>/share                      /home/share            cifs    user=admin,password=abc123.,file_mode=0777,dir_mode=0777,noperm       0  0

## Step 7: Configure SSHD on slave machines
Start the SSH daemon service:

    sudo systemctl start sshd
    sudo systemctl enable sshd
    exit
    ssh-keygen
    ssh-copy-id -i ~/.ssh/id_rsa.pub admin@<master>

# How to run the executor
The supported arguments of the executable jar are the following:

      -ms,--mastershare
    The shared folder residing on the master node that is visible to all slave nodes.

      -smf,--slavemountfolder
    The folder, local to slave nodes, where the master shared folder is mount.

      -u,--user
    The user name used to connect to remote slave nodes.
  
      -p,--password
    The password of the user used to connect to remote slave nodes.

      -in,--input
    The folder, relative to the common shared folder, from which the S2 products are to be processed.

      -out,--output
    The folder, relative to the master shared folder, to which the processed products will be written to.

      -sop,--slaveOp
    The name of the SNAP operator to be executed on each of the slave nodes.
  
      -sargs,--slaveOpArgs
    Parameters of the SNAP operator to be executed on each of the slave nodes.

      -mop,--masterOp
    The name of the SNAP operator to be executed on the master node.

      -margs,--masterOpArgs
    Parameters of the SNAP operator to be executed on the master node.
  
      -w,--wait
    How long (in minutes) the master node should wait for a slave node to complete its execution.

An example of using this command line tool:

    java -jar s2tbx-cep-1.0.jar -ms \\masternode\share -smf /mnt/share -u <user> -p <password> -in products -out . -w 10 
                                -sop NdviOp 
                                -sargs "-PnirFactor=1.0F -PnirSourceBand=B3 -PredFactor=1.0F -PredSourceBand=B4" 
                                -mop Mosaic 
                                -margs "-Pcombine=OR -Pcrs=EPSG:32634 -PeastBound=22.30 -PnorthBound=44.55 -PwestBound=24.45 -PsouthBound=43.45 -PpixelSizeX=0.001 -PpixelSizeY=0.001 -Presampling=Nearest -Porthorectify=false"

This example is just to illustrate the invocation syntax. For the Mosaic operator to work, an XML descriptor should be provided to it instead of the command line parameters.
