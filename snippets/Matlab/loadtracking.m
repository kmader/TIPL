% infile is the name of the file to read in, track1.csv for example

f=importdata([infile '_raw.txt']);
ffil=fopen([infile '_names.txt'],'r');
fnames=textscan(ffil,'%d,%s');
fclose(ffil);
framename=@(frm) fnames{2}{fnames{1}==frm};
%for i=6:8
%    f.data(:,i)=f.data(:,i)-mean(f.data(:,i));
%end
[junk,torder]=sort(f.data(:,1));

lmat=@(c) f.data(:,c)*ones(1,2)+[f.data(:,c+5)*0,f.data(:,c+5)]; % plot all chains
cmat=@(c,ch) f.data(torder(f.data(torder,2)==ch),c);
hold off
plot3(lmat(7)',lmat(8)',lmat(9)','-')
hold on
%%
minChainLength=2;
%%
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


%% the things needed for further tracking
poslist=@(frame) f.data(f.data(:,1)==frame,7:9)
nposlist=@(frame) f.data(f.data(:,1)==frame,7:9)+f.data(f.data(:,1)==frame,[7:9]+5)
framelist=sort(unique(f.data(:,1)));