function score=match_points(A,B)
min_dist=zeros(size(A,1)+size(B,1),1);
for i=1:size(A,1)
    distMat=(B-ones(size(B,1),1)*A(i,:)).^2;
    distMat(:,3)=distMat(:,3)*0.01; % 10 times less important
    min_dist(i)=min(sum(distMat,2));
end
for j=1:size(B,1)
    distMat=(A-ones(size(A,1),1)*B(j,:)).^2;
    distMat(:,3)=distMat(:,3)*0.01; % 10 times less important
    min_dist(j+size(A,1))=min(sum(distMat,2));
end
score=min_dist;