%% quickRegister finds the center of rotation and rotation axis position for a dataset

A=poslist(1);
%B=frot(poslist(1),pi/4,[0,0,0]);
B=poslist(2);

forceCent=[251.0,251.0,0]; % dk31g constriction
forceCent=[226.0,226.0,0]; % dk31m obstacle

centA=mean(A,1);
centB=mean(B,1);
A=A-ones(size(A,1),1)*centA;
B=B-ones(size(B,1),1)*centB;
tryTh=linspace(0,2*pi,50);
tryScores=arrayfun(@(th) mean(match_points(A,frot(B,th,[0,0,0]))),tryTh);
figure(2)
plot(tryTh*180/pi,tryScores)

A0=A;
B2=B;
%% in silico tests (sythetic rotation to be corrected)
rthe=pi/30
%poslist=@(frame) frot(poslist(0),rthe*(mod(frame,3)-1),mean(poslist(0)));%+30*rand(3,length(poslist(0)));
%nposlist=@(frame) frot(poslist(0),rthe*(frame+1),mean(poslist(0)));

%%
%poslist=@(frame) f.data(f.data(:,1)==frame,6:8)
%nposlist=@(frame) poslist(frame+1);

%% test dist keep function

distKeep=@(inDist) (inDist>240 | inDist<20);
%distKeep=@(inDist) (inDist>0) & (inDist<300);
close all;
A0=poslist(framelist(1));
Adist=sqrt((A0(:,1)-forceCent(1)).^2+(A0(:,2)-forceCent(2)).^2);
plot3(A0(:,1),A0(:,2),A0(:,3),'r+');
A0(distKeep(Adist),:)=[];
hold on
plot3(A0(:,1),A0(:,2),A0(:,3),'b.');

%% 
close all;
A0=poslist(framelist(1));

figure(1)
hold off
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
Adist=sqrt((A0(:,1)-forceCent(1)).^2+(A0(:,2)-forceCent(2)).^2);
A0(distKeep(Adist),:)=[];
pause(0.1)

outList=fopen('rotations.txt','w')

fprintf(outList,'ImageA,ImageB,A_B_Angle,CumSum_Angle,MeanPointDistance\n')
outFitAll=[];
outFitCumAll=[];


for nstart=1:(size(framelist))
    
    A=poslist(framelist(nstart));
    B=nposlist(framelist(nstart));
    Adist=sqrt((A(:,1)-250).^2+(A(:,2)-250).^2);
    A(distKeep(Adist),:)=[];
    
    Bdist=sqrt((B(:,1)-250).^2+(B(:,2)-250).^2);
    B(distKeep(Bdist),:)=[];
    fixB=@(x) ones(size(B,1),1)*[x(2:3),0]+frot(B-ones(size(B,1),1)*[x(2:3),0],x(1),[0,0,0]);
    % force the center position, comment to unforce
    fixB=@(x) ones(size(B,1),1)*forceCent+frot(B-ones(size(B,1),1)*forceCent,x(1),[0,0,0]);
    % guess theta
    tryTh=linspace(-pi,pi,201);
    tryScores=arrayfun(@(th) mean(match_points(A,fixB([th,mean(B,1)]))),tryTh);
    [bScore,bInd]=min(tryScores)
    
    outFit=fminsearch(@(x) mean(match_points(A,fixB(x))),[tryTh(bInd),mean(B,1)])
    meanDistFitScore=mean(match_points(A,fixB(outFit)));
    th=outFit(1);
    cPos=outFit(2:4);
    outFitAll(:,nstart)=outFit;
    
    
    gTh=0; % guess the starting angle from the last fit
    if (nstart>1)
        gTh=outFitCumAll(1,nstart-1)
    end
    
    outFitCum=fminsearch(@(x) mean(match_points(A0,fixB(x))),[gTh+th,outFit(2:4)])
    outFitCumAll(:,nstart)=outFitCum;
    
    B2=fixB(outFitCum);
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
    pause(0.1)
    if nstart<size(framelist)
        nextFrameName=framename(framelist(nstart+1));
    else
        nextFrameName=framename(framelist(nstart)+1);
    end
    fprintf(outList,[framename(framelist(nstart)) ',' nextFrameName ',' num2str(th*180/pi) ',' num2str(outFitCum(1)*180/pi) ',' num2str(meanDistFitScore) '\n'])
end
fclose(outList)

%%

%% write transform to file
for i=1:size(outFitCumAll,2)
    rthe=outFitCumAll(1,i)
    tranfile=[framename(framelist(i)) '_tf.dat']
    dlmwrite(tranfile,[forceCent;[cos(rthe),sin(rthe),0];[-sin(rthe),cos(rthe),0];[0,0,1]],',');
    %ty=fopen(tranfile,'w')
    %fprintf(ty,'%f,',[forceCent,[cos(rthe),sin(rthe),0];[-sin(rthe),cos(rthe),0],[0,0,1]])
    %fclose(ty);
end
