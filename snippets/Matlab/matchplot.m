%% init stuff
addpath('~/Documents/work/')
f=importdata('cortmatchcheck.csv_raw.txt');
ffil=fopen('cortmatchc0.csv_names.txt','r');
fnames=textscan(ffil,'%d,%s');
fclose(ffil);
framename=@(frm) fnames{2}{fnames{1}==frm};
%for i=6:8
%    f.data(:,i)=f.data(:,i)-mean(f.data(:,i));
%end
[junk,torder]=sort(f.data(:,1));

lmat=@(c) f.data(:,c)*ones(1,2)+[f.data(:,c+5)*0,f.data(:,c+5)]; % plot all chains
cmat=@(c,ch) f.data(torder(f.data(torder,2)==ch),c);




%%
hold off
plot3(lmat(7)',lmat(8)',lmat(9)','-')
hold on
%%
minChainLength=2;
curCentPos=[226,226,0];
%% plot the chains with the correct minimum length
hold off
chainlist=unique(f.data(:,2)');
k=jet;
kf=@(x) k(mod(round((x-1)/length(chainlist)*size(k,1)),size(k,1))+1,:);
hold off
for cind=1:length(chainlist)
    
    curch=chainlist(cind);
    if length(cmat(7,curch)')>minChainLength
        plot3(cmat(7,curch)',cmat(8,curch)',cmat(9,curch)','Color',kf(cind))
    
        hold on
    end
end
hold off
%% plot the chains with the correct minimum length in polar coordinates
hold off
chainlist=unique(f.data(:,2)');
k=jet;
kf=@(x) k(mod(round((x-1)/length(chainlist)*size(k,1)),size(k,1))+1,:);
hold off
for cind=1:length(chainlist)
    
    curch=chainlist(cind);
    if length(cmat(7,curch)')>minChainLength
        tempRmat=sqrt((cmat(7,curch)-curCentPos(1)).^2+(cmat(8,curch)-curCentPos(2)).^2);
        plot(tempRmat',cmat(9,curch)','Color',kf(cind))
    
        hold on
    end
end
hold off

%% plot the chains with the correct minimum length in polar coordinates
cfmat=@(c,frm,ch) f.data((torder(f.data(torder,2)==ch) & (f.data(torder,1)==frm)),c);
hold off
chainlist=unique(f.data(:,2)');
k=jet;
kf=@(x) k(mod(round((x-1)/length(chainlist)*size(k,1)),size(k,1))+1,:);
hold off
for frmind=1:length(framelist)
    for cind=1:length(chainlist)
        
        curch=chainlist(cind);
        if length(cmat(7,curch)')>minChainLength
            tempRmat=sqrt((cmat(7,curch)-curCentPos(1)).^2+(cmat(8,curch)-curCentPos(2)).^2);
            plot(tempRmat',cmat(9,curch)','Color',kf(cind))

            hold on
        end
    end
    hold off
    pause(0.1)
end


%% plot the bubble deviation from the start position
hold off
bplot=1;
for cind=1:length(chainlist)
    curch=chainlist(cind)
    
    xv=cmat(7,curch)';
    yv=cmat(8,curch)';
    zv=cmat(9,curch)';
    if length(xv)>minChainLength
        plot3(xv-xv(1),yv-yv(1),zv-zv(1),'+-','Color',kf(cind))
        hold on
        bplot=bplot+1'
    end
    
end
hold off
title('From Start Bubble Movements')

%% chain statistics
hold off
chstats=[]
for cind=1:length(chainlist)
    xv=cmat(7,curch)';
    yv=cmat(8,curch)';
    zv=cmat(9,curch)';
    clen=cmat(4,curch)';
    displen=sqrt((xv(end)-xv(1)).^2+(yv(end)-yv(1)).^2+(zv(end)-zv(1)).^2);
    curch=chainlist(cind);
    plot3(xv-xv(1),yv-yv(1),zv-zv(1),'+-','Color',kf(cind))
    hold on
    %                 number of images, xm, ym, zm, path length, displacement
    chstats=[chstats;[length(xv),mean(xv),mean(yv),mean(zv),sum(clen),displen]];
end
hold off
title('From Start Bubble Movements')


%%
obsdist=@(x,y,z) sqrt((x-220).^2+(y-220).^2+(z-265).^2);

robsd=obsdist(f.data(:,7),f.data(:,8),265*0+f.data(:,9));
vmag=sqrt(sum(f.data(:,12:14).^2,2))
vthet=atan2(f.data(:,14)./vmag,sqrt(sum(f.data(:,12:13).^2,2))./vmag);
hold off;
cplot(f.data(:,9),vmag)
xlabel('Z Position')
ylabel('Velocity Magnitude')

hold on
%plot(robsd(vthet>0),vmag(vthet>0),'b.')


%% rigid register

poslist=@(frame) f.data(f.data(:,1)==frame,6:8)
nposlist=@(frame) f.data(f.data(:,1)==frame,6:8)+f.data(f.data(:,1)==frame,[6:8]+5)
% synthetic rotation
rthe=pi/30
%poslist=@(frame) frot(poslist(0),rthe*frame,mean(poslist(0)));
%poslist=@(frame) frot(poslist(0),rthe*(mod(frame,3)-1),mean(poslist(0)));%+30*rand(3,length(poslist(0)));
%nposlist=@(frame) poslist(frame+1);
%nposlist=@(frame) frot(poslist(0),rthe*(frame+1),mean(poslist(0)));
%framelist=sort(unique(f.data(:,1)));
close all
figure(1)
hold off
A0=poslist(framelist(1));
subplot(2,2,1)
plot3(A0(:,1),A0(:,2),A0(:,3),'r+');
title('Corrected 3d')
cor3D=axis;
hold on;
subplot(2,2,3)
plot3(A0(:,1),A0(:,2),A0(:,3),'r+');
title('Uncorrected 3d')
hold on;
subplot(2,2,2)
plot(A0(:,1),A0(:,2),'r+');
title('Corrected 2d');
cor2D=axis;
hold on;
subplot(2,2,4)
plot(A0(:,1),A0(:,2),'r+');
title('Unorrected 2d')
hold on;
T=zeros(4,4,size(framelist)-1);
% B=T*A
% C=T*B
% C=T*T*A
for nstart=1:(size(framelist)-1)
    
    A=poslist(framelist(nstart));
    B=nposlist(framelist(nstart));
    %if nstart>1
    %     Bno=([B ones(size(B,1),1)]');
   % 
   %     Bno = inv(T(:,:,nstart-1)) * Bno;
   %     Bno = Bno';
   %     Bno = Bno(:,1:3); % remove the 1's added earlier
   % end
    makePosZero=1;
    if makePosZero
        A(:,3)=1;
        B(:,3)=1;
    end
    
    T(:,:,nstart)=rigid_transform_3D(B,A); % we want to undo the rigid transformation
    
    T(:,:,nstart)=estimateRigidTransform(A',B');
    
    A=poslist(framelist(nstart));B=nposlist(framelist(nstart));
    
    makeZero=0;
    if makeZero
        T(3,4,nstart)=0; % do not correct z
        T(3,1:3,nstart)=0;
        T(1:3,3,nstart)=0;
        T(3,3,nstart)=1;
    end
    
    Tf=T;
    for i=2:nstart
        Tf(:,:,i)=Tf(:,:,i-1)*Tf(:,:,i);
    end
    
    
    B2=([B ones(size(B,1),1)]');
    
    B2 = Tf(:,:,nstart) * B2;
    B2 = B2';
    B2 = B2(:,1:3); % remove the 1's added earlier
    %f.data(f.data(:,1)==framelist(nstart+1),6:8)=B2(:,1:3); % insert
    figure(1)
    subplot(2,2,1)
    plot3(B2(:,1),B2(:,2),B2(:,3),'b+') % plot B2 corrected back to A0
    axis(cor3D);
    hold on;
    subplot(2,2,2)
    plot(B2(:,1),B2(:,2),'b+') % plot B2 corrected back to A0
    axis(cor2D);
    hold on;
    subplot(2,2,3)
    plot3(B(:,1),B(:,2),B(:,3),'b+') % plot B2 corrected back to A0
    axis(cor3D);
    hold on;
    subplot(2,2,4)
    plot(B(:,1),B(:,2),'b+') % plot B2 corrected back to A0
    axis(cor2D);
    hold on;
    %figure(2)
    %plot3(B2(:,1),B2(:,2),B2(:,3),'r-') % plot B2 corrected back to A0
    
    pause(0.1)
end

% cumulative T
Tf=T;
for i=2:size(Tf,3)
    Tf(:,:,i)=Tf(:,:,i-1)*Tf(:,:,i);
end
Tf(:,:,end+1)=Tf(:,:,end);

%%
close all;plot(180/pi*atan2(squeeze(Tf(1,2,1:end)),squeeze(Tf(1,1,1:end))))

%% write transform to file
for i=1:size(Tf,3)
    tranfile=[framename(framelist(i)) '_tf.dat']
    ty=fopen(tranfile,'w')
    fprintf(ty,'%f,',T(:,:,i))
    fclose(ty);
end


%% compare normal bubbles with corrected bubbles
close all
figure(1)
hold off
subplot(2,2,1)
hold off
subplot(2,2,2)
hold off
subplot(2,2,3)
hold off
subplot(2,2,4)
hold off
chainlist=unique(f.data(:,2)');
k=jet;
kf=@(x) k(mod(round((x-1)/length(chainlist)*size(k,1)),size(k,1))+1,:);

% normal bubbles
for cind=1:length(chainlist)
    curch=chainlist(cind);
    ptlist=[cmat(6,curch),cmat(7,curch),cmat(8,curch),cmat(1,curch)];
    nstd=std(ptlist);
    subplot(2,2,1);
    plot3(ptlist(:,1),ptlist(:,2),ptlist(:,3),'Color',kf(cind));
    title('Before Correction')
    hold on
    subplot(2,2,2);
    plot3(ptlist(:,1),ptlist(:,2),ptlist(:,3),'.','Color',kf(cind));
    subplot(2,2,4);
    plot(ptlist(:,1),ptlist(:,2),'.','Color',kf(cind));
    hold on
    subplot(2,2,2);
    for npt=1:size(ptlist,1)
        tMatInd=find(ptlist(npt,4)==framelist);
        tempTmat=Tf(:,:,tMatInd);
        corPt=tempTmat*[ptlist(npt,1:3) 1]';
        ptlist(npt,1:3)=corPt(1:3);
    end
    
    
    plot3(ptlist(:,1),ptlist(:,2),ptlist(:,3),'Color',kf(cind));
    pstd=std(ptlist);
    title(sprintf('Before %f, After %f',[sum(nstd(1:2).^2),sum(pstd(1:2).^2)]))
    hold on
    subplot(2,2,3);
    plot3(ptlist(:,1),ptlist(:,2),ptlist(:,3),'Color',kf(cind));
    title('After Correction')
    hold on
    subplot(2,2,4);
    plot(ptlist(:,1),ptlist(:,2),'Color',kf(cind));
    pause(1)
end
hold off


%% twist correction, to correct twisting we find the
poslist=@(frame) f.data(f.data(:,1)==frame,6:8)
dirlist=@(frame) f.data(f.data(:,1)==frame,[6:8]+5);
for nstart=1:(size(framelist)-1)
    A0=poslist(nstart);
    V0=dirlist(nstart);
    plot(A0(:,1),V0(:,1),'r.',A0(:,2),atan2(V0(:,2),V0(:,1)),'g.');%,A0(:,3),V0(:,3),'b.')
    pause(1)
end

%%
%% twist correction, to correct twisting we find the
poslist=@(frame) f.data(f.data(:,1)==frame,6:8)
dirlist=@(frame) f.data(f.data(:,1)==frame,[6:8]+5);
xd=linspace(11,420,60);
yd=xd(1:2:end);
wx=mean(diff(xd))/1;
wy=mean(diff(yd));
lenx=size(xd,2);
leny=size(yd,2);
[mx,my]=meshgrid(yd,xd);
mx=mx(:);my=my(:);
omat=zeros(size(mx));
omatsum=zeros(size(mx));
for nstart=1:(size(framelist))
    A0=poslist(nstart);
    V0=dirlist(nstart);
    cxv=(A0(:,1)*ones(size(mx))')';
    cyv=(A0(:,2)*ones(size(mx))')';
    ctv=(atan2(V0(:,2),V0(:,1))*ones(size(mx))')';
    ctv=(sqrt(V0(:,2).^2+V0(:,1).^2)*ones(size(mx))')';
    ctv=(ctv);
    cx=mx*ones(size(A0(:,1)))';
    cy=my*ones(size(A0(:,1)))';
    
    cvf=exp(-1*((cx-cxv).^2/wx.^2+(cy-cyv).^2/wy.^2));
    
    cmat=sum(cvf.*ctv,2);
    cmatsum=sum(cvf,2);
    omat=omat+cmat;
    omatsum=omatsum+cmatsum;
    subplot(2,2,1);
    omage=reshape(cmat./cmatsum,[lenx,leny]);
    imagesc(yd,xd,omage);
    colorbar
    subplot(2,2,3);
    plot(xd,sum(omage,2),'r.');
    subplot(2,2,4)
    plot(yd,sum(omage,1),'r.');
    title('Sign of Theta (atan(y/x))')
    %saveas(gca,sprintf('Animation/sigtheta.%05d.tif',nstart))
    %oImage(nstart)=getframe();
    pause(0.5)
end
%imagesc(reshape(omat./omatsum,[lenx,leny]))

