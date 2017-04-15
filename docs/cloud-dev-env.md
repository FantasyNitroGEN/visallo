# Cloud Development Environment

An easy way to get started with Visallo development is to instantiate a cloud-based instance using one of 
our pre-built cloud templates. The templates enable one to spin-up a Linux instance with a remote desktop interface and 
all required dependencies pre-installed. The sections below describe the process for creating a development 
environment for each of our supported cloud platforms.

## Amazon Web Services Cloud Formation

Cloud Formation is an Amazon AWS technology that makes it easy to spin up a pre-configured infrastructure within the AWS
environment. The following sections describe how to use the Visallo Cloud Formation script to setup a cloud based 
development environment.

### Dependencies

The Visallo Cloud Development Environment for AWS requires a Remote Desktop Protocal (RDP) client and a 
[free subscription to CentOS 7](https://aws.amazon.com/marketplace/pp/B00O7WM7QW?qid=1482963029123&sr=0-1&ref_=srh_res_product_title) 
within the AWS Marketplace. We recommend using the built-in RDP client for Microsoft operating systems or the Microsoft 
 Remote Desktop client for Mac, available in the [App Store](https://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0ahUKEwiOmeKM9JfRAhXB6oMKHaNjDzYQFgg1MAA&url=https%3A%2F%2Fitunes.apple.com%2Fus%2Fapp%2Fmicrosoft-remote-desktop%2Fid715768417%3Fmt%3D12&usg=AFQjCNFv2RdDR7gkKRrSqnnyekA95vBVgw).

### Setup Procedure

Use the Visallo Cloud Formation script to start a Visallo cloud development environment as an AWS EC2 instance using the 
following steps:

1. [Download](https://github.com/visallo/visallo/blob/master/cloud/dev/visallo-cloud-formation.yaml) the Cloud Formation script
1. Upload the script through the [Cloud Formation](https://aws.amazon.com/cloudformation/) section of the Amazon Web 
Services Console for your AWS account.
1. Fill in appropriate parameter values. Defaults will work in most cases.
1. After clicking create on the last screen, you may need to wait as many as 20 minutes for the stack to be instantiated 
and all of the Visallo software and required dependencies to be installed. There is no indication for when this is done, 
so you'll really need to wait.
1. Click the `Outputs` tab of the Cloud Formation stack you just created and note the public IP address.
1. Create an SSH tunnel to the public IP address noted in the previous step, forwarding port 3389. `ssh -L 3389:localhost:3389 centos@<public_ip>`
1. Create an RDP connection to `localhost`, with a username of `visallo` and the password you entered earlier as a 
parameter to the Cloud Formation script

### After First Login

After completing the steps in the previous section, you should have a remote desktop session to the Visallo CentOS instance. 
You will find an entry for IntelliJ IDEA in the Programming section of the Applications menu. You will be prompted with
some setup steps the first time you run it. Please uncheck the box for creating a `Desktop Entry` for one of the steps as 
it's already been created for you.

At this point, you should have a development environment containing everything you need to work through the 
[Getting Started](getting-started.md) guide.